package com.oceanview.web.servlet;

import com.oceanview.model.Reservation;
import com.oceanview.service.ReservationService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.awt.Color;
import java.io.IOException;
import java.text.DecimalFormat;

@WebServlet("/api/invoice")
public class InvoiceServlet extends HttpServlet {

    private ReservationService service;
    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    @Override
    public void init() {
        service = new ReservationService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String idStr = req.getParameter("reservationId");
        if (idStr == null) {
            resp.sendError(400, "Missing ID");
            return;
        }

        Reservation r = service.getById(Integer.parseInt(idStr));
        if (r == null) {
            resp.sendError(404, "Not Found");
            return;
        }

        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "inline; filename=\"INV-" + r.getReservationNumber() + ".pdf\"");

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float width = page.getMediaBox().getWidth();
            float height = page.getMediaBox().getHeight();
            float margin = 50;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                
                // 1. HEADER BANNER (Dark Blue Gradient look)
                cs.setNonStrokingColor(new Color(2, 119, 189)); // Primary Blue
                cs.addRect(0, height - 200, width, 200);
                cs.fill();

                // 2. HEADER TEXT
                cs.setNonStrokingColor(Color.WHITE);
                writeText(cs, margin, height - 80, Standard14Fonts.FontName.HELVETICA_BOLD, 36, "INVOICE");
                
                writeText(cs, margin, height - 120, Standard14Fonts.FontName.HELVETICA_BOLD, 14, "OceanView Resort");
                writeText(cs, margin, height - 135, Standard14Fonts.FontName.HELVETICA, 10, "Galle, Sri Lanka");
                writeText(cs, margin, height - 150, Standard14Fonts.FontName.HELVETICA, 10, "info@oceanview.com");

                // Header Labels (Right Side)
                float rightAlign = width - 200;
                writeText(cs, rightAlign, height - 120, Standard14Fonts.FontName.HELVETICA_BOLD, 10, "Invoice No:");
                writeText(cs, rightAlign + 80, height - 120, Standard14Fonts.FontName.HELVETICA, 10, ": INV-" + r.getReservationNumber());
                
                writeText(cs, rightAlign, height - 135, Standard14Fonts.FontName.HELVETICA_BOLD, 10, "Issue Date:");
                writeText(cs, rightAlign + 80, height - 135, Standard14Fonts.FontName.HELVETICA, 10, ": " + java.time.LocalDate.now());

                // 3. BILLED TO SECTION
                float y = height - 250;
                cs.setNonStrokingColor(Color.BLACK);
                writeText(cs, margin, y, Standard14Fonts.FontName.HELVETICA_BOLD, 11, "Billed To");
                writeText(cs, margin, y - 15, Standard14Fonts.FontName.HELVETICA, 10, safe(r.getGuestName()));
                writeText(cs, margin, y - 30, Standard14Fonts.FontName.HELVETICA, 10, safe(r.getGuestEmail()));
                writeText(cs, margin, y - 45, Standard14Fonts.FontName.HELVETICA, 10, safe(r.getGuestContactNumber()));

                // 4. TABLE HEADER
                y -= 80;
                cs.setNonStrokingColor(new Color(240, 245, 250)); // Light blue-gray background
                cs.addRect(margin, y - 5, width - (2 * margin), 20);
                cs.fill();
                
                cs.setNonStrokingColor(new Color(2, 119, 189));
                writeText(cs, margin + 10, y, Standard14Fonts.FontName.HELVETICA_BOLD, 10, "Description");
                writeText(cs, margin + 250, y, Standard14Fonts.FontName.HELVETICA_BOLD, 10, "Qty (Nights)");
                writeText(cs, margin + 350, y, Standard14Fonts.FontName.HELVETICA_BOLD, 10, "Unit Price");
                writeText(cs, width - margin - 50, y, Standard14Fonts.FontName.HELVETICA_BOLD, 10, "Total");

                // 5. TABLE CONTENT
                y -= 30;
                cs.setNonStrokingColor(Color.BLACK);
                writeText(cs, margin + 10, y, Standard14Fonts.FontName.HELVETICA_BOLD, 10, safe(r.getRoomType()));
                writeText(cs, margin + 10, y - 12, Standard14Fonts.FontName.HELVETICA, 9, "Room No: " + safe(r.getRoomNumber()));
                
                writeText(cs, margin + 250, y, Standard14Fonts.FontName.HELVETICA, 10, String.valueOf(r.getNights()));
                writeText(cs, margin + 350, y, Standard14Fonts.FontName.HELVETICA, 10, "$ " + df.format(r.getRatePerNight()));
                writeText(cs, width - margin - 60, y, Standard14Fonts.FontName.HELVETICA, 10, "$ " + df.format(r.getSubtotal()));

                // Decorative Line
                cs.setStrokingColor(new Color(200, 200, 200));
                cs.moveTo(margin, y - 25);
                cs.lineTo(width - margin, y - 25);
                cs.stroke();

                // 6. TOTALS SECTION (Right Aligned)
                y -= 60;
                float summaryX = width - 200;
                writeSummaryRow(cs, summaryX, y, "Sub Total", "$ " + df.format(r.getSubtotal()), false);
                writeSummaryRow(cs, summaryX, y - 15, "Tax (10%)", "$ " + df.format(r.getTax()), false);
                writeSummaryRow(cs, summaryX, y - 30, "Discount", "-$ " + df.format(r.getDiscount()), false);

                // 7. GRAND TOTAL BOX
                y -= 60;
                cs.setNonStrokingColor(new Color(2, 119, 189));
                cs.addRect(summaryX - 20, y - 10, 170, 25);
                cs.fill();
                
                cs.setNonStrokingColor(Color.WHITE);
                writeText(cs, summaryX - 10, y, Standard14Fonts.FontName.HELVETICA_BOLD, 12, "Grand Total");
                writeText(cs, width - margin - 60, y, Standard14Fonts.FontName.HELVETICA_BOLD, 12, "$ " + df.format(r.getTotalAmount()));

                // FOOTER
                writeText(cs, margin, 100, Standard14Fonts.FontName.HELVETICA_BOLD, 12, "Thank you for your Business");
                writeText(cs, margin, 85, Standard14Fonts.FontName.HELVETICA, 9, "Payment Terms: Cash/Card/Bank Transfer accepted.");
            }
            doc.save(resp.getOutputStream());
        }
    }

    private void writeText(PDPageContentStream cs, float x, float y, Standard14Fonts.FontName font, int size, String text) throws IOException {
        cs.beginText();
        cs.setFont(new PDType1Font(font), size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private void writeSummaryRow(PDPageContentStream cs, float x, float y, String label, String value, boolean bold) throws IOException {
        cs.setNonStrokingColor(bold ? Color.BLACK : new Color(100, 100, 100));
        writeText(cs, x, y, bold ? Standard14Fonts.FontName.HELVETICA_BOLD : Standard14Fonts.FontName.HELVETICA, 10, label);
        writeText(cs, x + 100, y, bold ? Standard14Fonts.FontName.HELVETICA_BOLD : Standard14Fonts.FontName.HELVETICA, 10, value);
    }

    private String safe(String s) { return s == null ? "-" : s; }
}