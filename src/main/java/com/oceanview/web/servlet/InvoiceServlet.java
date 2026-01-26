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
import java.io.IOException;
import java.text.DecimalFormat;

@WebServlet("/api/invoice")
public class InvoiceServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
	private ReservationService service;

    @Override
    public void init() {
        service = new ReservationService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = Integer.parseInt(req.getParameter("reservationId"));
        Reservation r = service.getById(id); 
        if (r == null) {
            resp.setStatus(404);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"success\":false,\"message\":\"Reservation not found\"}");
            return;
        }

        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "inline; filename=\"INV-" + r.getReservationNumber() + ".pdf\"");

        DecimalFormat df = new DecimalFormat("0.00");

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float x = 50;
                float y = 780;
                float lh = 16;

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);

                cs.newLineAtOffset(x, y);
                cs.showText("OceanView Resort - Invoice");
                cs.endText();

                y -= 2 * lh;

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);

                cs.newLineAtOffset(x, y);
                cs.showText("Invoice No: INV-" + r.getReservationNumber());
                cs.endText();

                y -= lh;

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);

                cs.newLineAtOffset(x, y);
                cs.showText("Guest: " + safe(r.getGuestName()) + " | " + safe(r.getGuestEmail()) + " | " + safe(r.getGuestContactNumber()));
                cs.endText();

                y -= lh;

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);

                cs.newLineAtOffset(x, y);
                cs.showText("Room: " + safe(r.getRoomNumber()) + " (" + safe(r.getRoomType()) + ")");
                cs.endText();

                y -= lh;

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);

                cs.newLineAtOffset(x, y);
                cs.showText("Dates: " + r.getCheckInDate() + "  ->  " + r.getCheckOutDate() + " | Nights: " + r.getNights());
                cs.endText();

                y -= 2 * lh;

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);

                cs.newLineAtOffset(x, y);
                cs.showText("Pricing");
                cs.endText();

                y -= lh;

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                cs.newLineAtOffset(x, y);
                cs.showText("Rate/Night: " + df.format(r.getRatePerNight()));
                cs.endText();

                y -= lh;

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
                cs.newLineAtOffset(x, y);
                cs.showText("Subtotal: " + df.format(r.getSubtotal()));
                cs.endText();

                y -= lh;

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
                cs.newLineAtOffset(x, y);
                cs.showText("Tax: " + df.format(r.getTax()) + " | Discount: " + df.format(r.getDiscount()));
                cs.endText();

                y -= lh;

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                cs.newLineAtOffset(x, y);
                cs.showText("Total: " + df.format(r.getTotalAmount()));
                cs.endText();
            }

            doc.save(resp.getOutputStream());
        }
    }

    private String safe(String s) { return s == null ? "-" : s; }
}
