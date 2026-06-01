package com.platformcommons.security.report;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.platformcommons.security.model.OWASPCategory;
import com.platformcommons.security.model.SeverityLevel;
import com.platformcommons.security.model.TestResult;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates a professional A4 PDF security report using OpenPDF (LGPL).
 */
@Component
public class PdfReportGenerator {

    // ── Brand colours ─────────────────────────────────────────────────────────
    private static final Color C_NAVY    = new Color(13,  27,  75);
    private static final Color C_BLUE    = new Color(25, 118, 210);
    private static final Color C_LIGHT   = new Color(232, 240, 254);
    private static final Color C_WHITE   = Color.WHITE;
    private static final Color C_DARK    = new Color(33,  33,  33);
    private static final Color C_MUTED   = new Color(117, 117, 117);
    private static final Color C_ROW_ALT = new Color(248, 249, 250);
    private static final Color C_DIV     = new Color(200, 200, 200);
    private static final Color C_CODE_BG = new Color(30,  30,  30);
    private static final Color C_CODE_FG = new Color(212, 212, 212);
    private static final Color C_FIX_BG  = new Color(22,  40,  70);

    // ── Severity badge colours ────────────────────────────────────────────────
    private static final Map<SeverityLevel, Color> SEV_COLOR = Map.of(
            SeverityLevel.CRITICAL, new Color(183, 28,  28),
            SeverityLevel.HIGH,     new Color(230, 81,   0),
            SeverityLevel.MEDIUM,   new Color(245, 127, 23),
            SeverityLevel.LOW,      new Color(25,  118, 210),
            SeverityLevel.INFO,     new Color(69,  90, 100),
            SeverityLevel.PASS,     new Color(46,  125,  50)
    );

    public String generateReport(List<TestResult> results, String targetUrl, String outputDir)
            throws Exception {
        new File(outputDir).mkdirs();
        String ts   = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String path = outputDir + File.separator + "owasp_report_" + ts + ".pdf";

        Document doc = new Document(PageSize.A4, 50, 50, 72, 62);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(path));
        writer.setPageEvent(new HeaderFooterEvent(targetUrl));
        doc.open();

        addCoverPage(doc, targetUrl, results);
        doc.newPage();
        addExecutiveSummary(doc, results, targetUrl);
        doc.newPage();
        addFindingsByCategory(doc, results);

        doc.close();
        return path;
    }

    // ─── Cover page ───────────────────────────────────────────────────────────

    private void addCoverPage(Document doc, String targetUrl, List<TestResult> results)
            throws Exception {

        doc.add(blankLines(4));

        Paragraph title = new Paragraph("OWASP SECURITY ASSESSMENT", font(FontFactory.HELVETICA_BOLD, 26, C_NAVY));
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);

        Paragraph sub = new Paragraph("Top 10 Vulnerability Report — Apache2", font(FontFactory.HELVETICA, 14, C_BLUE));
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(20);
        doc.add(sub);

        // Meta table
        PdfPTable meta = new PdfPTable(2);
        meta.setWidthPercentage(72);
        meta.setHorizontalAlignment(Element.ALIGN_CENTER);
        meta.setWidths(new float[]{38, 62});

        row(meta, "Target URL",   targetUrl);
        row(meta, "Report Date",  LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm")));
        row(meta, "Standard",     "OWASP Top 10 : 2021");
        row(meta, "Total Checks", String.valueOf(results.size()));
        long issueCount = results.stream().filter(r -> !r.isPassed()).count();
        row(meta, "Issues Found", String.valueOf(issueCount));
        doc.add(meta);

        doc.add(blankLines(2));
        doc.add(severityBanner(results));
        doc.add(blankLines(3));

        Paragraph disc = new Paragraph(
                "CONFIDENTIAL  –  For authorised security testing only.",
                font(FontFactory.HELVETICA_OBLIQUE, 9, C_MUTED));
        disc.setAlignment(Element.ALIGN_CENTER);
        doc.add(disc);
    }

    // ─── Executive summary ────────────────────────────────────────────────────

    private void addExecutiveSummary(Document doc, List<TestResult> results, String targetUrl)
            throws Exception {

        doc.add(sectionTitle("Executive Summary"));

        long total  = results.size();
        long issues = results.stream().filter(r -> !r.isPassed()).count();
        long passed = total - issues;

        Paragraph intro = new Paragraph(
                "This automated assessment evaluated " + targetUrl
                + " against OWASP Top 10:2021. " + total + " checks were run — "
                + passed + " passed and " + issues + " issue(s) require attention.",
                body());
        intro.setSpacingAfter(12);
        doc.add(intro);

        // Severity breakdown
        doc.add(new Paragraph("Findings by Severity", boldBody()));
        doc.add(blankLines(1));

        PdfPTable sevTable = new PdfPTable(3);
        sevTable.setWidthPercentage(65);
        sevTable.setWidths(new float[]{38, 22, 40});
        headerRow(sevTable, "Severity", "Count", "Action Required");

        for (SeverityLevel sev : List.of(SeverityLevel.CRITICAL, SeverityLevel.HIGH,
                SeverityLevel.MEDIUM, SeverityLevel.LOW, SeverityLevel.INFO)) {
            long cnt = results.stream().filter(r -> !r.isPassed() && r.getSeverity() == sev).count();
            Color badge = cnt > 0 ? SEV_COLOR.get(sev) : C_DIV;
            badgeRow(sevTable, sev.getLabel(), String.valueOf(cnt), actionLabel(sev), badge);
        }
        doc.add(sevTable);

        doc.add(blankLines(2));

        // Category summary
        doc.add(new Paragraph("Findings by OWASP Category", boldBody()));
        doc.add(blankLines(1));

        PdfPTable catTable = new PdfPTable(3);
        catTable.setWidthPercentage(100);
        catTable.setWidths(new float[]{22, 56, 22});
        headerRow(catTable, "Category", "Name", "Issues");

        int i = 0;
        for (OWASPCategory cat : OWASPCategory.values()) {
            long cnt  = results.stream().filter(r -> !r.isPassed() && r.getCategory() == cat).count();
            Color row = i++ % 2 == 0 ? C_WHITE : C_ROW_ALT;
            PdfPCell c1 = cell(cat.getId(),   row, body());
            PdfPCell c2 = cell(cat.getName(), row, body());
            PdfPCell c3 = cnt > 0
                    ? cell(String.valueOf(cnt), SEV_COLOR.getOrDefault(worstSev(results, cat), C_MUTED), wbody())
                    : cell("0", row, body());
            catTable.addCell(c1); catTable.addCell(c2); catTable.addCell(c3);
        }
        doc.add(catTable);
    }

    // ─── Detailed findings ────────────────────────────────────────────────────

    private void addFindingsByCategory(Document doc, List<TestResult> results) throws Exception {
        doc.add(sectionTitle("Detailed Findings"));

        Map<OWASPCategory, List<TestResult>> grouped = results.stream()
                .collect(Collectors.groupingBy(TestResult::getCategory,
                        TreeMap::new, Collectors.toList()));

        for (Map.Entry<OWASPCategory, List<TestResult>> e : grouped.entrySet()) {
            OWASPCategory   cat  = e.getKey();
            List<TestResult> list = e.getValue();
            long issues = list.stream().filter(r -> !r.isPassed()).count();

            doc.add(blankLines(1));
            Paragraph ch = new Paragraph(cat.getFullTitle(), font(FontFactory.HELVETICA_BOLD, 13, C_NAVY));
            ch.setSpacingAfter(4);
            doc.add(ch);

            Paragraph cd = new Paragraph(cat.getDescription(), font(FontFactory.HELVETICA_OBLIQUE, 9, C_MUTED));
            cd.setSpacingAfter(8);
            doc.add(cd);

            if (issues == 0) {
                Paragraph ok = new Paragraph("✓  No issues detected in this category.",
                        font(FontFactory.HELVETICA_BOLD, 10, SEV_COLOR.get(SeverityLevel.PASS)));
                ok.setSpacingAfter(10);
                doc.add(ok);
                continue;
            }

            for (TestResult r : list) {
                if (r.isPassed()) continue;
                renderFinding(doc, r);
            }
        }
    }

    private void renderFinding(Document doc, TestResult r) throws Exception {
        // Title bar: finding name | severity badge
        PdfPTable bar = new PdfPTable(new float[]{76, 24});
        bar.setWidthPercentage(100);
        bar.setSpacingBefore(12);

        PdfPCell nameC = cell(r.getTestName(), C_LIGHT, boldBody());
        nameC.setPaddingTop(7); nameC.setPaddingBottom(7);
        bar.addCell(nameC);

        PdfPCell sevC = cell(r.getSeverity().getLabel(),
                SEV_COLOR.getOrDefault(r.getSeverity(), C_MUTED), wbody());
        sevC.setHorizontalAlignment(Element.ALIGN_CENTER);
        sevC.setVerticalAlignment(Element.ALIGN_MIDDLE);
        sevC.setPaddingTop(7); sevC.setPaddingBottom(7);
        bar.addCell(sevC);
        doc.add(bar);

        labelValue(doc, "Endpoint:",    r.getEndpoint());
        labelValue(doc, "Description:", r.getDescription());

        // Screenshot
        if (r.getScreenshotPng() != null && r.getScreenshotPng().length > 0) {
            try {
                Image img = Image.getInstance(r.getScreenshotPng());
                img.scaleToFit(490, 280);
                img.setSpacingBefore(4); img.setSpacingAfter(4);
                doc.add(new Paragraph("Evidence Screenshot:", boldBody()));
                doc.add(img);
            } catch (Exception ex) {
                addCodeBox(doc, r.getEvidence(), C_CODE_BG, C_CODE_FG);
            }
        } else if (r.getEvidence() != null && !r.getEvidence().isBlank()) {
            doc.add(new Paragraph("Evidence:", boldBody()));
            addCodeBox(doc, r.getEvidence(), C_CODE_BG, C_CODE_FG);
        }

        // Recommendation
        if (r.getRecommendation() != null && !r.getRecommendation().isBlank()) {
            doc.add(new Paragraph("Recommendation:", boldBody()));
            addCodeBox(doc, r.getRecommendation(), C_FIX_BG, new Color(150, 220, 150));
        }

        // Thin divider
        PdfPTable div = new PdfPTable(1);
        div.setWidthPercentage(100);
        div.setSpacingBefore(6);
        PdfPCell dc = new PdfPCell(new Phrase(""));
        dc.setBorder(Rectangle.BOTTOM);
        dc.setBorderColorBottom(C_DIV);
        dc.setBorderWidthBottom(0.5f);
        dc.setPadding(0);
        div.addCell(dc);
        doc.add(div);
    }

    // ─── Building blocks ──────────────────────────────────────────────────────

    private void addCodeBox(Document doc, String text, Color bg, Color fg) throws Exception {
        String safe = text == null ? "" : (text.length() > 1400 ? text.substring(0, 1400) + "\n…[truncated]" : text);
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(4); t.setSpacingAfter(8);
        PdfPCell c = new PdfPCell(new Phrase(safe, font(FontFactory.COURIER, 8, fg)));
        c.setBackgroundColor(bg);
        c.setPadding(9);
        c.setBorder(Rectangle.NO_BORDER);
        t.addCell(c);
        doc.add(t);
    }

    private void labelValue(Document doc, String label, String value) throws Exception {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + " ", boldBody()));
        p.add(new Chunk(value != null ? value : "", body()));
        p.setSpacingAfter(4);
        doc.add(p);
    }

    private PdfPTable severityBanner(List<TestResult> results) throws Exception {
        PdfPTable t = new PdfPTable(5);
        t.setWidthPercentage(88);
        t.setHorizontalAlignment(Element.ALIGN_CENTER);
        for (SeverityLevel sev : List.of(SeverityLevel.CRITICAL, SeverityLevel.HIGH,
                SeverityLevel.MEDIUM, SeverityLevel.LOW, SeverityLevel.INFO)) {
            long cnt = results.stream().filter(r -> !r.isPassed() && r.getSeverity() == sev).count();
            PdfPCell c = new PdfPCell();
            c.setBackgroundColor(SEV_COLOR.get(sev));
            c.setPadding(14);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph p = new Paragraph();
            p.setAlignment(Element.ALIGN_CENTER);
            p.add(new Chunk(String.valueOf(cnt), font(FontFactory.HELVETICA_BOLD, 22, C_WHITE)));
            p.add(Chunk.NEWLINE);
            p.add(new Chunk(sev.getLabel(),      font(FontFactory.HELVETICA, 9, C_WHITE)));
            c.addElement(p);
            t.addCell(c);
        }
        return t;
    }

    private Paragraph sectionTitle(String text) {
        Paragraph p = new Paragraph(text, font(FontFactory.HELVETICA_BOLD, 17, C_NAVY));
        p.setSpacingBefore(10); p.setSpacingAfter(6);
        return p;
    }

    private Paragraph blankLines(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append("\n");
        return new Paragraph(sb.toString(), body());
    }

    // ── Table helpers ─────────────────────────────────────────────────────────

    private void row(PdfPTable t, String key, String value) {
        PdfPCell kc = cell(key,   C_LIGHT, boldBody()); kc.setPadding(7);
        PdfPCell vc = cell(value, C_WHITE, body());     vc.setPadding(7);
        kc.setBorderColor(C_DIV); vc.setBorderColor(C_DIV);
        t.addCell(kc); t.addCell(vc);
    }

    private void headerRow(PdfPTable t, String... cols) {
        for (String col : cols) {
            PdfPCell c = cell(col, C_NAVY, wbody());
            c.setPadding(8);
            t.addCell(c);
        }
    }

    private void badgeRow(PdfPTable t, String label, String count, String action, Color badge) {
        PdfPCell lc = cell(label,  C_ROW_ALT, body());    lc.setPadding(7);
        PdfPCell cc = cell(count,  badge,     wbody());   cc.setPadding(7); cc.setHorizontalAlignment(Element.ALIGN_CENTER);
        PdfPCell ac = cell(action, C_ROW_ALT, body());    ac.setPadding(7);
        t.addCell(lc); t.addCell(cc); t.addCell(ac);
    }

    private PdfPCell cell(String text, Color bg, Font fnt) {
        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "", fnt));
        c.setBackgroundColor(bg);
        c.setBorderColor(C_DIV);
        c.setPadding(6);
        return c;
    }

    // ── Font helpers ──────────────────────────────────────────────────────────

    private Font font(String name, int size, Color color) {
        Font f = FontFactory.getFont(name, size);
        f.setColor(color);
        return f;
    }

    private Font body()      { return font(FontFactory.HELVETICA,         10, C_DARK);  }
    private Font boldBody()  { return font(FontFactory.HELVETICA_BOLD,    10, C_DARK);  }
    private Font wbody()     { return font(FontFactory.HELVETICA_BOLD,    10, C_WHITE); }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private SeverityLevel worstSev(List<TestResult> results, OWASPCategory cat) {
        return results.stream()
                .filter(r -> !r.isPassed() && r.getCategory() == cat)
                .map(TestResult::getSeverity)
                .max(Comparator.comparingInt(SeverityLevel::getWeight))
                .orElse(SeverityLevel.INFO);
    }

    private String actionLabel(SeverityLevel sev) {
        return switch (sev) {
            case CRITICAL -> "Immediate Action";
            case HIGH     -> "Urgent";
            case MEDIUM   -> "Should Fix";
            case LOW      -> "Consider Fixing";
            default       -> "Advisory";
        };
    }

    // ─── Page event (header / footer) ─────────────────────────────────────────

    private static class HeaderFooterEvent extends PdfPageEventHelper {
        private final String url;
        HeaderFooterEvent(String url) { this.url = url; }

        @Override
        public void onEndPage(PdfWriter writer, Document doc) {
            PdfContentByte cb = writer.getDirectContent();
            cb.saveState();

            // Top bar
            cb.setColorFill(C_NAVY);
            cb.rectangle(0, doc.top() + 14, doc.getPageSize().getWidth(), 30);
            cb.fill();

            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    phrase8("OWASP Security Report  |  " + url, Color.WHITE),
                    doc.left(), doc.top() + 23, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    phrase8("CONFIDENTIAL", new Color(255, 200, 0)),
                    doc.right(), doc.top() + 23, 0);

            // Bottom bar
            cb.setColorFill(new Color(240, 240, 240));
            cb.rectangle(0, 0, doc.getPageSize().getWidth(), 24);
            cb.fill();

            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    phrase8("Generated by OWASP Security Tester  •  platformcommons.com",
                            new Color(110, 110, 110)),
                    doc.left(), 8, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    phrase8("Page " + writer.getPageNumber(), new Color(110, 110, 110)),
                    doc.right(), 8, 0);

            cb.restoreState();
        }

        private Phrase phrase8(String text, Color color) {
            Font f = FontFactory.getFont(FontFactory.HELVETICA, 8);
            f.setColor(color);
            return new Phrase(text, f);
        }
    }
}
