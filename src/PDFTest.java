import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.FileOutputStream;

public class PDFTest {

    public static void main(String[] args) {

        try {

            Document document = new Document();

            PdfWriter.getInstance(
                    document,
                    new FileOutputStream("DavidReport.pdf")
            );

            document.open();

            document.add(
                    new Paragraph(
                            "Hello David!"
                    )
            );

            document.add(
                    new Paragraph(
                            "Your PDF system is working successfully."
                    )
            );

            document.close();

            System.out.println(
                    "PDF Generated Successfully!"
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}