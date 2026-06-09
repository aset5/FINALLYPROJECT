package com.example.internship.services;

import com.example.internship.models.Application;
import com.example.internship.models.Internship;
import com.example.internship.models.University;
import com.example.internship.models.User;
import com.example.internship.util.QrCodeImageGenerator;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class CertificateService {

    private final CertificateNumberService certificateNumberService;
    private final PublicBaseUrlResolver publicBaseUrlResolver;

    public CertificateService(
            CertificateNumberService certificateNumberService,
            PublicBaseUrlResolver publicBaseUrlResolver) {
        this.certificateNumberService = certificateNumberService;
        this.publicBaseUrlResolver = publicBaseUrlResolver;
    }

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("ru"));
    private static final DateTimeFormatter DATE_SHORT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final Color NAVY = new Color(25, 55, 109);
    private static final Color GOLD = new Color(184, 134, 11);
    private static final Color GOLD_LIGHT = new Color(252, 246, 227);
    private static final Color STAMP_RED = new Color(178, 34, 34);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color BODY = new Color(51, 65, 85);
    private static final Color DARK = new Color(30, 41, 59);

    public byte[] generate(Application application) {
        return generate(application, null);
    }

    public byte[] generate(Application application, HttpServletRequest request) {
        try {
            String baseUrl = publicBaseUrlResolver.resolve(request);
            return generatePdf(application, baseUrl);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось сформировать сертификат", e);
        }
    }

    private byte[] generatePdf(Application application, String publicBaseUrl)
            throws DocumentException, java.io.IOException {
        User student = application.getStudent();
        Internship program = application.getInternship();
        String universityName = program.getUniversity() != null
                ? program.getUniversity().getName()
                : "INTERN.PRO";

        String studentName = student.getFullName() != null && !student.getFullName().isBlank()
                ? student.getFullName()
                : student.getUsername();

        LocalDate date = application.getCompletedAt() != null
                ? application.getCompletedAt().toLocalDate()
                : LocalDate.now();

        String certNumber = certificateNumberService.buildNumber(application);
        String verifyUrl = certificateNumberService.buildVerifyUrl(publicBaseUrl, certNumber);

        BaseFont bfRegular = loadFont(BaseFont.TIMES_ROMAN);
        BaseFont bfBold = loadFont(BaseFont.TIMES_BOLD);
        BaseFont bfItalic = loadFont(BaseFont.TIMES_ITALIC);

        StudentCertificateData studentData = buildStudentData(student, application, studentName);

        CertificateLayout layout = new CertificateLayout(
                universityName, certNumber, verifyUrl, date, studentData, bfRegular, bfBold);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 48, 48, 48, 48);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(layout);
            document.open();

            Font brandFont = new Font(bfBold, 11, Font.NORMAL, GOLD);
            Font brandSubFont = new Font(bfRegular, 9, Font.NORMAL, MUTED);
            Font titleFont = new Font(bfBold, 30, Font.NORMAL, NAVY);
            Font subtitleFont = new Font(bfItalic, 12, Font.NORMAL, MUTED);
            Font bodyFont = new Font(bfRegular, 11, Font.NORMAL, BODY);
            Font nameFont = new Font(bfBold, 24, Font.NORMAL, NAVY);
            Font programFont = new Font(bfBold, 13, Font.NORMAL, DARK);
            Font uniFont = new Font(bfRegular, 12, Font.NORMAL, MUTED);
            Font metaFont = new Font(bfRegular, 10, Font.NORMAL, MUTED);

            Paragraph brand = new Paragraph("INTERN.PRO", brandFont);
            brand.setAlignment(Element.ALIGN_CENTER);
            brand.setSpacingAfter(2);
            document.add(brand);

            Paragraph brandSub = new Paragraph("Платформа стажировок и карьеры", brandSubFont);
            brandSub.setAlignment(Element.ALIGN_CENTER);
            brandSub.setSpacingAfter(14);
            document.add(brandSub);

            Paragraph title = new Paragraph("СЕРТИФИКАТ", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph sub = new Paragraph("о прохождении программы профессионального обучения", subtitleFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(16);
            document.add(sub);

            Paragraph body = new Paragraph("Настоящим удостоверяется, что", bodyFont);
            body.setAlignment(Element.ALIGN_CENTER);
            body.setSpacingAfter(8);
            document.add(body);

            Paragraph name = new Paragraph(studentName, nameFont);
            name.setAlignment(Element.ALIGN_CENTER);
            name.setSpacingAfter(6);
            document.add(name);

            Paragraph completed = new Paragraph("успешно завершил(а) программу обучения", bodyFont);
            completed.setAlignment(Element.ALIGN_CENTER);
            completed.setSpacingAfter(6);
            document.add(completed);

            Paragraph programTitle = new Paragraph("«" + program.getTitle() + "»", programFont);
            programTitle.setAlignment(Element.ALIGN_CENTER);
            programTitle.setSpacingAfter(4);
            document.add(programTitle);

            Paragraph uni = new Paragraph("при " + universityName, uniFont);
            uni.setAlignment(Element.ALIGN_CENTER);
            uni.setSpacingAfter(12);
            document.add(uni);

            if (application.getFinalGradePercent() != null) {
                String gradeText = "Итоговая оценка: " + application.getFinalGradePercent() + "%";
                if (application.getGradeLetter() != null && !application.getGradeLetter().isBlank()) {
                    gradeText += "  ·  " + application.getGradeLetter();
                }
                Font gradeFont = new Font(bfBold, 11, Font.NORMAL, NAVY);
                Paragraph grade = new Paragraph(gradeText, gradeFont);
                grade.setAlignment(Element.ALIGN_CENTER);
                grade.setSpacingAfter(10);
                document.add(grade);
            }

            Paragraph meta = new Paragraph(
                    "Дата выдачи: " + DATE_FMT.format(date) + "   ·   Рег. № " + certNumber,
                    metaFont);
            meta.setAlignment(Element.ALIGN_CENTER);
            meta.setSpacingAfter(28);
            document.add(meta);

            Font signLabel = new Font(bfRegular, 9, Font.NORMAL, MUTED);
            Font signTitle = new Font(bfBold, 10, Font.NORMAL, NAVY);

            PdfPTable footer = new PdfPTable(3);
            footer.setWidthPercentage(90);
            footer.setWidths(new float[]{38, 24, 38});

            footer.addCell(signCell("Руководитель программы", universityName, signLabel, signTitle, Element.ALIGN_LEFT));

            PdfPCell center = new PdfPCell();
            center.setBorder(Rectangle.NO_BORDER);
            center.setMinimumHeight(55);
            footer.addCell(center);

            footer.addCell(signCell("Платформа INTERN.PRO", "Официальный реестр", signLabel, signTitle, Element.ALIGN_RIGHT));

            document.add(footer);

            Paragraph verify = new Paragraph(
                    "Проверка подлинности: отсканируйте QR или введите номер на сайте  ·  " + certNumber,
                    new Font(bfRegular, 8, Font.NORMAL, new Color(148, 163, 184)));
            verify.setAlignment(Element.ALIGN_CENTER);
            verify.setSpacingBefore(8);
            document.add(verify);

            document.close();
            return out.toByteArray();
        }
    }

    private static StudentCertificateData buildStudentData(User student, Application application, String displayName) {
        University studentUni = student.getUniversity();
        String studentUniversity = studentUni != null ? studentUni.getName() : "не указан";
        String appliedAt = application.getAppliedAt() != null
                ? DATE_SHORT.format(application.getAppliedAt().toLocalDate())
                : "—";
        String completedAt = application.getCompletedAt() != null
                ? DATE_SHORT.format(application.getCompletedAt().toLocalDate())
                : "—";

        return new StudentCertificateData(
                displayName,
                String.valueOf(student.getId()),
                student.getUsername(),
                dashIfBlank(student.getEmail()),
                dashIfBlank(student.getPhone()),
                studentUniversity,
                appliedAt,
                completedAt
        );
    }

    private static PdfPCell signCell(String role, String org, Font labelFont, Font titleFont, int align)
            throws DocumentException {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(4);
        cell.setHorizontalAlignment(align);

        Paragraph p = new Paragraph();
        p.setAlignment(align);
        p.add(new Chunk(role + "\n", labelFont));
        p.add(new Chunk(org, titleFont));
        cell.addElement(p);

        Paragraph line = new Paragraph(" ");
        line.setSpacingBefore(22);
        cell.addElement(line);
        return cell;
    }

    private static String dashIfBlank(String value) {
        return value != null && !value.isBlank() ? value.trim() : "не указан";
    }

    private static BaseFont loadFont(String name) throws DocumentException, java.io.IOException {
        try {
            return BaseFont.createFont(name, "CP1251", BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            return BaseFont.createFont(name, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        }
    }

    private record StudentCertificateData(
            String fullName,
            String studentId,
            String username,
            String email,
            String phone,
            String university,
            String enrolledAt,
            String completedAt
    ) {}

    /** Рисует рамку, водяной знак, печать и блок данных студента поверх страницы. */
    private static class CertificateLayout extends PdfPageEventHelper {
        private final String universityName;
        private final String certNumber;
        private final String verifyUrl;
        private final LocalDate date;
        private final StudentCertificateData studentData;
        private final BaseFont bfRegular;
        private final BaseFont bfBold;

        CertificateLayout(
                String universityName,
                String certNumber,
                String verifyUrl,
                LocalDate date,
                StudentCertificateData studentData,
                BaseFont bfRegular,
                BaseFont bfBold) {
            this.universityName = truncate(universityName, 28);
            this.certNumber = certNumber;
            this.verifyUrl = verifyUrl;
            this.date = date;
            this.studentData = studentData;
            this.bfRegular = bfRegular;
            this.bfBold = bfBold;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Rectangle page = document.getPageSize();
            PdfContentByte under = writer.getDirectContentUnder();
            PdfContentByte over = writer.getDirectContent();

            drawWatermark(under, page);
            drawBorders(over, page);
            drawCornerOrnaments(over, page);
            drawStudentPanel(over, page);
            drawSeal(over, page.getWidth() / 2, 88, universityName);
            drawPlatformBadge(over, page.getWidth() - 110, page.getHeight() - 72);
            drawVerifyQr(over, page);
        }

        private void drawVerifyQr(PdfContentByte canvas, Rectangle page) {
            try {
                BufferedImage qrBitmap = QrCodeImageGenerator.generate(verifyUrl, 180);
                Image qr = Image.getInstance(qrBitmap, null);
                float size = 68f;
                qr.scaleToFit(size, size);
                float x = 52f;
                float y = 62f;
                qr.setAbsolutePosition(x, y);
                canvas.saveState();
                canvas.setColorStroke(GOLD);
                canvas.setLineWidth(1.2f);
                canvas.roundRectangle(x - 4, y - 4, size + 8, size + 8, 6);
                canvas.stroke();
                canvas.addImage(qr);
                canvas.restoreState();

                Font hintFont = new Font(bfRegular, 7, Font.NORMAL, MUTED);
                ColumnText.showTextAligned(
                        canvas,
                        Element.ALIGN_LEFT,
                        new Phrase("Проверка QR", hintFont),
                        x,
                        y - 10,
                        0);
            } catch (Exception ignored) {
                /* QR optional */
            }
        }

        private void drawStudentPanel(PdfContentByte canvas, Rectangle page) {
            float panelW = 248;
            float panelH = 198;
            float x = page.getWidth() - panelW - 52;
            float y = page.getHeight() - panelH - 58;

            canvas.saveState();
            canvas.setColorFill(Color.WHITE);
            canvas.roundRectangle(x, y, panelW, panelH, 8);
            canvas.fill();

            canvas.setColorStroke(GOLD);
            canvas.setLineWidth(2f);
            canvas.roundRectangle(x, y, panelW, panelH, 8);
            canvas.stroke();

            canvas.setColorStroke(NAVY);
            canvas.setLineWidth(0.6f);
            canvas.roundRectangle(x + 3, y + 3, panelW - 6, panelH - 6, 6);
            canvas.stroke();
            canvas.restoreState();

            Font titleFont = new Font(bfBold, 11, Font.NORMAL, NAVY);
            Font labelFont = new Font(bfBold, 9, Font.NORMAL, GOLD);
            Font valueFont = new Font(bfRegular, 10, Font.NORMAL, DARK);

            drawStudentPanelContent(canvas, x, y, panelW, panelH, labelFont, valueFont, titleFont);
        }

        private void drawStudentPanelContent(
                PdfContentByte canvas,
                float x,
                float y,
                float panelW,
                float panelH,
                Font labelFont,
                Font valueFont,
                Font titleFont) {
            try {
                float cy = y + panelH - 22;
                ColumnText.showTextAligned(
                        canvas,
                        Element.ALIGN_CENTER,
                        new Phrase("ДАННЫЕ ОБУЧАЮЩЕГОСЯ", titleFont),
                        x + panelW / 2,
                        cy,
                        0);
                cy -= 18;
                cy = drawLine(canvas, "ФИО:", studentData.fullName(), x + 14, cy, labelFont, valueFont);
                cy = drawLine(canvas, "ID:", studentData.studentId(), x + 14, cy, labelFont, valueFont);
                cy = drawLine(canvas, "Логин:", studentData.username(), x + 14, cy, labelFont, valueFont);
                cy = drawLine(canvas, "Email:", studentData.email(), x + 14, cy, labelFont, valueFont);
                cy = drawLine(canvas, "Телефон:", studentData.phone(), x + 14, cy, labelFont, valueFont);
                cy = drawLine(canvas, "ВУЗ:", studentData.university(), x + 14, cy, labelFont, valueFont);
                cy = drawLine(canvas, "Зачисление:", studentData.enrolledAt(), x + 14, cy, labelFont, valueFont);
                drawLine(canvas, "Завершение:", studentData.completedAt(), x + 14, cy, labelFont, valueFont);
            } catch (Exception ignored) {
                /* last resort */
            }
        }

        private float drawLine(
                PdfContentByte canvas,
                String label,
                String value,
                float x,
                float y,
                Font labelFont,
                Font valueFont) throws DocumentException {
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new Phrase(label, labelFont), x, y, 0);
            ColumnText.showTextAligned(
                    canvas,
                    Element.ALIGN_LEFT,
                    new Phrase(truncate(value, 32), valueFont),
                    x + 72,
                    y,
                    0);
            return y - 16;
        }

        private void drawWatermark(PdfContentByte canvas, Rectangle page) {
            PdfGState gs = new PdfGState();
            gs.setFillOpacity(0.05f);
            canvas.saveState();
            canvas.setGState(gs);
            canvas.setColorFill(NAVY);
            try {
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                canvas.beginText();
                canvas.setFontAndSize(bf, 64);
                canvas.showTextAligned(
                        Element.ALIGN_CENTER,
                        "INTERN.PRO",
                        page.getWidth() / 2,
                        page.getHeight() / 2,
                        32);
                canvas.endText();
            } catch (Exception ignored) {
                /* optional */
            }
            canvas.restoreState();
        }

        private void drawBorders(PdfContentByte canvas, Rectangle page) {
            float m = 24;
            float w = page.getWidth() - 2 * m;
            float h = page.getHeight() - 2 * m;

            // Только обводка — без заливки поверх текста
            canvas.setColorStroke(GOLD);
            canvas.setLineWidth(2.5f);
            canvas.roundRectangle(m, m, w, h, 10);
            canvas.stroke();

            canvas.setColorStroke(NAVY);
            canvas.setLineWidth(0.8f);
            canvas.roundRectangle(m + 6, m + 6, w - 12, h - 12, 8);
            canvas.stroke();
        }

        private void drawCornerOrnaments(PdfContentByte canvas, Rectangle page) {
            canvas.setColorStroke(GOLD);
            canvas.setLineWidth(1.5f);
            float len = 36;
            float inset = 38;
            float maxX = page.getWidth() - inset;
            float maxY = page.getHeight() - inset;

            canvas.moveTo(inset, inset + len);
            canvas.lineTo(inset, inset);
            canvas.lineTo(inset + len, inset);
            canvas.stroke();

            canvas.moveTo(maxX - len, inset);
            canvas.lineTo(maxX, inset);
            canvas.lineTo(maxX, inset + len);
            canvas.stroke();

            canvas.moveTo(inset, maxY - len);
            canvas.lineTo(inset, maxY);
            canvas.lineTo(inset + len, maxY);
            canvas.stroke();

            canvas.moveTo(maxX - len, maxY);
            canvas.lineTo(maxX, maxY);
            canvas.lineTo(maxX, maxY - len);
            canvas.stroke();
        }

        private void drawSeal(PdfContentByte canvas, float cx, float cy, String org) {
            canvas.saveState();
            float outerR = 48;
            float innerR = 40;

            canvas.setColorStroke(STAMP_RED);
            canvas.setLineWidth(2.2f);
            canvas.circle(cx, cy, outerR);
            canvas.stroke();

            canvas.setLineWidth(1f);
            canvas.circle(cx, cy, innerR);
            canvas.stroke();

            try {
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, "CP1251", BaseFont.NOT_EMBEDDED);
                canvas.setColorFill(STAMP_RED);
                canvas.beginText();
                canvas.setFontAndSize(bf, 6.5f);
                canvas.showTextAligned(Element.ALIGN_CENTER, org.toUpperCase(), cx, cy + 14, 0);
                canvas.endText();
                canvas.beginText();
                canvas.setFontAndSize(bf, 8.5f);
                canvas.showTextAligned(Element.ALIGN_CENTER, "ПОДТВЕРЖДЕНО", cx, cy + 1, 0);
                canvas.endText();
                canvas.beginText();
                canvas.setFontAndSize(bf, 6.5f);
                canvas.showTextAligned(Element.ALIGN_CENTER, DATE_SHORT.format(date), cx, cy - 12, 0);
                canvas.endText();
                canvas.beginText();
                canvas.setFontAndSize(bf, 5.5f);
                canvas.showTextAligned(Element.ALIGN_CENTER, certNumber, cx, cy - 22, 0);
                canvas.endText();
            } catch (Exception ignored) {
                /* optional */
            }
            canvas.restoreState();
        }

        private void drawPlatformBadge(PdfContentByte canvas, float x, float y) {
            canvas.saveState();
            canvas.setColorFill(NAVY);
            canvas.circle(x, y, 26);
            canvas.fill();
            canvas.setColorStroke(GOLD);
            canvas.setLineWidth(1.5f);
            canvas.circle(x, y, 26);
            canvas.stroke();
            try {
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                canvas.setColorFill(Color.WHITE);
                canvas.beginText();
                canvas.setFontAndSize(bf, 7.5f);
                canvas.showTextAligned(Element.ALIGN_CENTER, "INTERN", x, y + 3, 0);
                canvas.showTextAligned(Element.ALIGN_CENTER, "PRO", x, y - 7, 0);
                canvas.endText();
            } catch (Exception ignored) {
                /* optional */
            }
            canvas.restoreState();
        }

        private static String truncate(String s, int max) {
            if (s == null) return "";
            if (s.length() <= max) return s;
            return s.substring(0, max - 1) + "...";
        }
    }
}
