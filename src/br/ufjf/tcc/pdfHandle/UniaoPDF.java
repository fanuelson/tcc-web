package br.ufjf.tcc.pdfHandle;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.lowagie.text.Document;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

public class UniaoPDF {

	public static void unirPDFsFichaAvaliacaoIndividual(int qtAvaliadores, int idAluno) {

		try {
			List<InputStream> pdfs = new ArrayList<InputStream>();
			for (int i = 0; i < qtAvaliadores; i++) {
				pdfs.add(new FileInputStream(Ata.PASTA_ARQUIVOS_TEMP + idAluno + "-" + i + ".pdf"));
			}

			OutputStream output = new FileOutputStream(Ata.PASTA_ARQUIVOS_TEMP+ Ata.FICHA_AVALIACAO_INDIVIDUAL + idAluno + ".pdf");
			UniaoPDF.concatPDFs(pdfs, output, true);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void unirFichaAvaliacaoFinalComFichaAvaliacaoIndividual(int idAluno) {

		try {

			List<InputStream> pdfs = new ArrayList<InputStream>();

			pdfs.add(new FileInputStream(Ata.PASTA_ARQUIVOS_TEMP + Ata.FICHA_AVALIACAO_FINAL + idAluno + ".pdf"));
			pdfs.add(new FileInputStream(Ata.PASTA_ARQUIVOS_TEMP + Ata.FICHA_AVALIACAO_INDIVIDUAL + idAluno + ".pdf"));

			OutputStream output = new FileOutputStream(Ata.PASTA_ARQUIVOS_TEMP + Ata.FICHA_COMPLETA + idAluno + ".pdf");

			UniaoPDF.concatPDFs(pdfs, output, true);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public static void unirPDFsExibicaoBanca(int qtMarcada, int idAluno) {

		try {
			List<InputStream> pdfs = new ArrayList<InputStream>();
			for (int i = 0; i < qtMarcada; i++) {
				pdfs.add(new FileInputStream(Ata.PASTA_ARQUIVOS_TEMP + Ata.COMPOSICAO_BANCA + idAluno + "-" + i + ".pdf"));
			}

			OutputStream output = new FileOutputStream(Ata.PASTA_ARQUIVOS_TEMP + Ata.COMPOSICAO_BANCA_FINAL + idAluno + ".pdf");
			UniaoPDF.concatPDFs(pdfs, output, true);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// JUNTA V�RIOS ARQUIVOS .PDF EM UM �NICO
	private static void concatPDFs(List<InputStream> streamOfPDFFiles,
			OutputStream outputStream, boolean paginate) {

		Document document = new Document();
		try {
			List<InputStream> pdfs = streamOfPDFFiles;
			List<PdfReader> readers = new ArrayList<PdfReader>();
			// int totalPages = 0;
			Iterator<InputStream> iteratorPDFs = pdfs.iterator();

			// Create Readers for the pdfs.
			while (iteratorPDFs.hasNext()) {
				InputStream pdf = iteratorPDFs.next();
				PdfReader pdfReader = new PdfReader(pdf);
				readers.add(pdfReader);
				// totalPages += pdfReader.getNumberOfPages();
			}
			// Create a writer for the outputstream
			PdfWriter writer = PdfWriter.getInstance(document, outputStream);

			document.open();
			BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA,
					BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
			PdfContentByte cb = writer.getDirectContent(); // Holds the PDF
			// data

			PdfImportedPage page;
			// int currentPageNumber = 0;
			int pageOfCurrentReaderPDF = 0;
			Iterator<PdfReader> iteratorPDFReader = readers.iterator();

			// Loop through the PDF files and add to the output.
			while (iteratorPDFReader.hasNext()) {
				PdfReader pdfReader = iteratorPDFReader.next();

				// Create a new page in the target for each source page.
				while (pageOfCurrentReaderPDF < pdfReader.getNumberOfPages()) {
					document.newPage();
					pageOfCurrentReaderPDF++;
					// currentPageNumber++;
					page = writer.getImportedPage(pdfReader,
							pageOfCurrentReaderPDF);
					cb.addTemplate(page, 0, 0);

					// Code for pagination.
					if (paginate) {
						cb.beginText();
						cb.setFontAndSize(bf, 9);
					
						cb.endText();
					}
				}
				pageOfCurrentReaderPDF = 0;
			}
			outputStream.flush();
			document.close();
			outputStream.close();

			for (int i = 0; i < pdfs.size(); i++)
				pdfs.get(i).close();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (document.isOpen())
				document.close();
			try {
				if (outputStream != null)
					outputStream.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

}
