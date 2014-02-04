package br.ufjf.tcc.controller;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.util.media.AMedia;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Window;

import br.ufjf.tcc.business.DepartamentoBusiness;
import br.ufjf.tcc.business.ParticipacaoBusiness;
import br.ufjf.tcc.business.TCCBusiness;
import br.ufjf.tcc.business.UsuarioBusiness;
import br.ufjf.tcc.library.FileManager;
import br.ufjf.tcc.model.Departamento;
import br.ufjf.tcc.model.Participacao;
import br.ufjf.tcc.model.TCC;
import br.ufjf.tcc.model.Usuario;

public class EditorTccController extends CommonsController {

	private TCCBusiness tccBusiness = new TCCBusiness();
	private Usuario tempUser = null;
	private TCC tcc = null;
	private Iframe iframe;
	private InputStream tccFile = null, extraFile = null;
	private AMedia pdf = null;
	private List<Departamento> departamentos;
	private List<Usuario> orientadores = new ArrayList<Usuario>();
	private boolean alunoEditBlock = false, canChangeMatricula = false,
			canEditUser = false, alunoVerified = false, tccFileChanged = false,
			extraFileChanged = false, hasSubtitulo = false;

	@Init
	public void init() {
		String tccId = Executions.getCurrent().getParameter("tcc");

		switch (getUsuario().getTipoUsuario().getIdTipoUsuario()) {
		case Usuario.ALUNO:
			TCC tempTCC = tccBusiness.getCurrentTCCByAuthor(getUsuario(),
					getCurrentCalendar());
			getUsuario().getTcc().clear();
			if (tempTCC != null) {
				getUsuario().getTcc().add(tempTCC);
				tcc = getUsuario().getTcc().get(0);
			} else
				redirectHome();

			break;

		case Usuario.ADMINISTRADOR:
		case Usuario.COORDENADOR:
			canChangeMatricula = true;

		case Usuario.SECRETARIA:
			canEditUser = true;

		default:
			if (tccId != null && tccId.trim().length() > 0) {
				tcc = tccBusiness.getTCCById(Integer.parseInt(tccId.trim()));
				alunoEditBlock = true;
			} else if (canEditUser) {
				tcc = new TCC();
				tcc.setAluno(new Usuario());
				if (getUsuario().getCurso() != null)
					tcc.getAluno().setCurso(getUsuario().getCurso());
				alunoEditBlock = false;
				canChangeMatricula = true;
			}

			if (tcc == null || !canEdit())
				redirectHome();

		}

		departamentos = new DepartamentoBusiness().getAll();
	}

	private boolean canEdit() {
		return (tcc.getOrientador() == null
				|| tcc.getOrientador().getIdUsuario() == getUsuario()
						.getIdUsuario()
				|| getUsuario().getTipoUsuario().getIdTipoUsuario() == Usuario.ADMINISTRADOR || (getUsuario()
				.getTipoUsuario().getIdTipoUsuario() == Usuario.COORDENADOR || (getUsuario()
				.getTipoUsuario().getIdTipoUsuario() == Usuario.SECRETARIA && tcc
				.getDataEnvioFinal() != null)
				&& getUsuario().getCurso().getIdCurso() == tcc.getAluno()
						.getCurso().getIdCurso()));
	}

	public TCC getTcc() {
		return tcc;
	}

	public void setTcc(TCC tcc) {
		this.tcc = tcc;
	}

	public boolean isAlunoEditBlock() {
		return alunoEditBlock;
	}

	public List<Departamento> getDepartamentos() {
		return departamentos;
	}

	public boolean getHasSubtitulo() {
		return hasSubtitulo;
	}

	@Command("setHasSubtitulo")
	@NotifyChange("hasSubtitulo")
	public void setHasSubtitulo() {
		hasSubtitulo = !hasSubtitulo;
		if (!hasSubtitulo)
			tcc.setSubNomeTCC(null);
	}

	public List<Usuario> getOrientadores() {
		return orientadores;
	}

	public boolean isCanChangeMatricula() {
		return canChangeMatricula;
	}

	public boolean getCanEditUser() {
		return canEditUser;
	}

	public boolean isUserSecretaria() {
		return (getUsuario().getTipoUsuario().getIdTipoUsuario() == Usuario.SECRETARIA);
	}

	public Usuario getTempUser() {
		return tempUser;
	}

	public void setTempUser(Usuario tempUser) {
		this.tempUser = tempUser;
	}

	@NotifyChange({ "tcc", "alunoExists" })
	@Command
	public void verifyAluno(@BindingParam("label") Label lbl) {
		lbl.setVisible(true);
		if (tcc.getAluno().getMatricula() != null
				&& tcc.getAluno().getMatricula().trim().length() > 0) {
			Usuario aluno = new UsuarioBusiness().getByMatricula(tcc.getAluno()
					.getMatricula().trim());
			if (aluno != null) {
				if (aluno.getTipoUsuario().getIdTipoUsuario() != Usuario.ALUNO
						|| getUsuario().getTipoUsuario().getIdTipoUsuario() != Usuario.ADMINISTRADOR
						&& aluno.getCurso().getIdCurso() != getUsuario()
								.getCurso().getIdCurso()) {
					lbl.setValue("Usuário existe mas não é um aluno ou pertence a outro curso.");
					alunoEditBlock = false;
					alunoVerified = false;
				} else {
					tcc.setAluno(aluno);
					lbl.setValue("Usuário já cadastrado.");
					alunoEditBlock = true;
					alunoVerified = true;
				}
			} else {
				if (getUsuario().getTipoUsuario().getIdTipoUsuario() != Usuario.ADMINISTRADOR) {
					alunoEditBlock = true;
					lbl.setValue("Usuário ainda não cadastrado.Faça o cadastro abaixo.");
					alunoVerified = true;
				} else {
					lbl.setValue("Usuário ainda não cadastrado.Cadastre ele no menu de Usuários.");
					alunoVerified = false;
				}
			}
		} else {
			alunoEditBlock = false;
			lbl.setValue("É necessário digitar a matrícula.");
			alunoVerified = false;
		}
	}

	@Command
	public void showTCC(@BindingParam("iframe") Iframe iframe) {
		this.iframe = iframe;

		AMedia pdf;
		if (tcc.getArquivoTCCBanca() == null) {
			InputStream is = FileManager.getFileInputSream("modelo.pdf");
			pdf = new AMedia("modelo.pdf", "pdf", "application/pdf", is);
		} else {
			tccFile = FileManager.getFileInputSream(tcc.getArquivoTCCBanca());
			pdf = new AMedia(tcc.getNomeTCC() + ".pdf", "pdf",
					"application/pdf", tccFile);
		}

		iframe.setContent(pdf);
	}

	@Command
	public void upload(@BindingParam("evt") UploadEvent evt) {
		if (!evt.getMedia().getName().contains(".pdf")) {
			Messagebox.show(
					"Este não é um arquivo válido! Apenas PDF são aceitos.",
					"Formato inválido", Messagebox.OK, Messagebox.INFORMATION);
			tccFile = null;
			return;
		}
		pdf = new AMedia(tcc.getNomeTCC(), "pdf", "application/pdf", evt
				.getMedia().getStreamData());
		tccFile = evt.getMedia().getStreamData();
		tccFileChanged = true;
	}

	@Command
	public void extraUpload(@BindingParam("evt") UploadEvent evt) {
		extraFile = evt.getMedia().getStreamData();
		extraFileChanged = true;
	}

	@Command
	public void selectedDepartamento(@BindingParam("dep") Comboitem combDep) {
		Departamento dep = (Departamento) combDep.getValue();
		tempUser = null;
		orientadores.clear();
		if (dep != null)
			orientadores = new UsuarioBusiness().getAllByDepartamento(dep);

		BindUtils.postNotifyChange(null, null, this, "tempUser");
		BindUtils.postNotifyChange(null, null, this, "orientadores");
	}

	// Editor Orientador

	@Command
	public void changeOrientador(@BindingParam("window") Window window) {
		window.doModal();
	}

	@Command
	public void selectOrientador(@BindingParam("window") Window window) {
		if (tempUser != null)
			if (!participacoesContains(tempUser)) {
				tcc.setOrientador(tempUser);
				BindUtils.postNotifyChange(null, null, this, "tcc");
			} else {
				Messagebox
						.show("Você escolheu um professor que já está incluído na Banca Examinadora. Se ele é seu Orientador, por favor retire-o da Banca antes.",
								"Inválido", Messagebox.OK, Messagebox.ERROR);
			}
		else
			Messagebox.show("Você não selecionou nenhum professor.", "Erro",
					Messagebox.OK, Messagebox.ERROR);
		window.setVisible(false);
	}

	private boolean participacoesContains(Usuario tempUser) {
		boolean find = false;

		for (Participacao p : tcc.getParticipacoes())
			if (p.getProfessor().getIdUsuario() == tempUser.getIdUsuario()) {
				find = true;
				break;
			}

		return find;
	}

	// Editor Banca
	@Command
	public void addToBanca() {
		if (tempUser != null) {
			if (!participacoesContains(tempUser)
					&& tempUser.getIdUsuario() != tcc.getOrientador()
							.getIdUsuario()) {
				Participacao p = new Participacao();
				p.setProfessor(tempUser);
				p.setTcc(tcc);
				if (tempUser.getTitulacao() != null)
					p.setTitulacao(tempUser.getTitulacao());
				tcc.getParticipacoes().add(p);
				BindUtils.postNotifyChange(null, null, this, "tcc");
			} else {
				Messagebox
						.show("Esse professor já está na lista ou é o orientador do TCC",
								"Erro", Messagebox.OK, Messagebox.ERROR);
			}
		}
	}

	@Command
	public void removeFromBanca(@BindingParam("participacao") Participacao p) {
		tcc.getParticipacoes().remove(p);
		BindUtils.postNotifyChange(null, null, this, "tcc");
	}

	@Command
	public void submitBanca(@BindingParam("window") Window window) {
		orientadores.clear();
		orientadores.add(tcc.getOrientador());
		window.setVisible(false);
	}

	// Submit do TCC
	@Command("submit")
	public void submit() {
		if (getUsuario().getTipoUsuario().getIdTipoUsuario() == Usuario.SECRETARIA
				&& (tcc.getArquivoTCCFinal() == null && !tccFileChanged)) {
			Messagebox.show("É necesário enviar o documento PDF.", "Erro",
					Messagebox.OK, Messagebox.ERROR);
			return;
		}
		if (getUsuario().getTipoUsuario().getIdTipoUsuario() != Usuario.ALUNO
				&& !alunoVerified) {
			Messagebox
					.show("Antes de enviar é necesário validar a matricula do aluno no botão de verificar.",
							"Erro", Messagebox.OK, Messagebox.ERROR);
			return;
		}
		if (!alunoEditBlock) {
			UsuarioBusiness usuarioBusiness = new UsuarioBusiness();
			if (usuarioBusiness.validate(tcc.getAluno(), null, false))
				if (!usuarioBusiness.salvar(tcc.getAluno())) {
					Messagebox.show(
							"Devido a um erro, o Autor não foi cadastrado.",
							"Erro", Messagebox.OK, Messagebox.ERROR);
					return;
				}
		}

		tcc.setDataEnvioBanca(new Timestamp(new Date().getTime()));
		if (tccBusiness.validate(tcc)) {
			if (tccFileChanged && tccFile != null) {
				iframe.setContent(pdf);
				savePDF();
				tccFileChanged = false;
				try {
					tccFile.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				tccFile = null;
			} else if (getUsuario().getTipoUsuario().getIdTipoUsuario() != Usuario.SECRETARIA
					&& tcc.getArquivoTCCBanca() == null)
				Messagebox
						.show("Você não enviou o documento PDF. Lembre-se de enviá-lo depois.",
								"Aviso", Messagebox.OK, Messagebox.INFORMATION);

			if (extraFileChanged && extraFile != null) {
				saveExtraFile();
				extraFileChanged = false;
				try {
					extraFile.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				extraFile = null;
			}

			if (tccBusiness.saveOrEdit(tcc)) {
				Messagebox.show("TCC atualizado com sucesso!");

				if (!new ParticipacaoBusiness().updateList(tcc)) {
					Messagebox
							.show("Não foi possível salvar as alterações da Banca Examinadora.",
									"Erro", Messagebox.OK, Messagebox.ERROR);
					return;
				}

			} else {
				Messagebox.show("Devido a um erro, o TCC não foi cadastrado.",
						"Erro", Messagebox.OK, Messagebox.ERROR);
			}

		} else {
			String errorMessage = "";
			for (String error : tccBusiness.getErrors())
				errorMessage += error;
			Messagebox.show(errorMessage, "Dados insuficientes / inválidos",
					Messagebox.OK, Messagebox.ERROR);
		}
	}

	@Command
	public void savePDF() {
		String newFileName = FileManager.saveFileInputSream(tccFile, "pdf");
		if (newFileName != null) {
			switch (getUsuario().getTipoUsuario().getIdTipoUsuario()) {
			case Usuario.SECRETARIA:
				FileManager.deleteFile(tcc.getArquivoTCCFinal());
				tcc.setArquivoTCCFinal(newFileName);
				break;
			default:
				FileManager.deleteFile(tcc.getArquivoTCCBanca());
				tcc.setArquivoTCCBanca(newFileName);
				break;
			}
		}
	}

	@Command
	public void saveExtraFile() {
		String newFileName = FileManager.saveFileInputSream(extraFile, ".pdf");
		if (newFileName != null) {
			FileManager.deleteFile(tcc.getArquivoExtraTCCBanca());
			tcc.setArquivoExtraTCCBanca(newFileName);
		}
	}

}
