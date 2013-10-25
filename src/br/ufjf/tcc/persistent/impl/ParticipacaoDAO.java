package br.ufjf.tcc.persistent.impl;

import java.util.List;

import org.hibernate.Query;

import br.ufjf.tcc.model.Participacao;
import br.ufjf.tcc.model.Usuario;
import br.ufjf.tcc.persistent.GenericoDAO;
import br.ufjf.tcc.persistent.IParticipacaoDAO;


public class ParticipacaoDAO extends GenericoDAO implements IParticipacaoDAO {

	@SuppressWarnings("unchecked")
	public List<Participacao> getParticipacoesByProfessor(Usuario professor) {
		List<Participacao> participacoes = null;
		try {
			Query query = getSession().createQuery(
					"SELECT p FROM Participacao AS p JOIN FETCH p.tcc AS t JOIN FETCH t.orientador JOIN FETCH t.aluno JOIN FETCH t.participacoes WHERE p.professor = :professor");
			query.setParameter("professor", professor);
			participacoes = query.list();
			getSession().close();
			return participacoes;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return participacoes;
	}

}
