package com.platformcommons.security.service;

import com.platformcommons.security.model.HttpProbeResult;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a styled HTTP request/response "screenshot" to PNG entirely in Java2D.
 * Works in headless mode – no browser required.
 *
 * The image mimics a dark-mode developer-tools Network panel.
 */
@Service
public class ScreenshotService {

    private static final int WIDTH     = 900;
    private static final int LINE_H    = 16;
    private static final int PADDING   = 12;
    private static final int TAB_STOP  = 160;

    // Color palette
    private static final Color BG           = new Color(30,  30,  30);
    private static final Color PANEL_BG     = new Color(40,  40,  40);
    private static final Color HEADER_BG    = new Color(20,  20,  90);
    private static final Color TEXT_DEFAULT = new Color(212, 212, 212);
    private static final Color TEXT_DIM     = new Color(140, 140, 140);
    private static final Color TEXT_KEY     = new Color(156, 220, 254);
    private static final Color TEXT_VALUE   = new Color(206, 145, 120);
    private static final Color TEXT_URL     = new Color(100, 220, 100);
    private static final Color STATUS_2XX   = new Color(40,  180,  40);
    private static final Color STATUS_3XX   = new Color(180, 140,  40);
    private static final Color STATUS_4XX   = new Color(220,  60,  60);
    private static final Color STATUS_5XX   = new Color(200,  40,  40);

    public byte[] capture(HttpProbeResult probe, String testName) {
        List<Line> lines = buildLines(probe, testName);
        int height = Math.max(300, lines.size() * LINE_H + PADDING * 4 + 40);

        BufferedImage img = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        applyHints(g);
        drawBackground(g, height);
        drawTitleBar(g, testName, probe);

        int y = 40 + PADDING;
        for (Line l : lines) {
            drawLine(g, l, y);
            y += LINE_H;
        }

        g.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    // ─── layout helpers ──────────────────────────────────────────────────────

    private List<Line> buildLines(HttpProbeResult p, String testName) {
        List<Line> out = new ArrayList<>();

        // ── REQUEST ──────────────────────────────────────────────────────────
        out.add(Line.section("── REQUEST ─────────────────────────────"));
        out.add(Line.kv(p.getMethod(), p.getUrl(), TEXT_DEFAULT, TEXT_URL));
        if (p.getRequestHeaders() != null) {
            p.getRequestHeaders().forEach((k, v) -> out.add(Line.kv(k + ":", v, TEXT_KEY, TEXT_VALUE)));
        }
        out.add(Line.blank());

        // ── RESPONSE ─────────────────────────────────────────────────────────
        out.add(Line.section("── RESPONSE ────────────────────────────"));
        if (p.isError()) {
            out.add(Line.plain("ERROR: " + p.getErrorMessage(), STATUS_4XX));
        } else {
            Color statusColor = statusColor(p.getStatusCode());
            out.add(Line.plain("HTTP  " + p.getStatusCode(), statusColor));
            if (p.getResponseHeaders() != null) {
                p.getResponseHeaders().forEach((k, v) -> out.add(Line.kv(k + ":", v, TEXT_KEY, TEXT_VALUE)));
            }
            if (p.getResponseBody() != null && !p.getResponseBody().isBlank()) {
                out.add(Line.blank());
                out.add(Line.section("── BODY (preview) ──────────────────────"));
                String body = p.getResponseBody().replaceAll("\r", "");
                String[] bodyLines = body.split("\n");
                int shown = Math.min(bodyLines.length, 20);
                for (int i = 0; i < shown; i++) {
                    String bl = bodyLines[i];
                    if (bl.length() > 110) bl = bl.substring(0, 110) + "…";
                    out.add(Line.plain(bl, TEXT_DEFAULT));
                }
                if (bodyLines.length > shown) {
                    out.add(Line.plain("… [" + (bodyLines.length - shown) + " more lines truncated]", TEXT_DIM));
                }
            }
        }
        out.add(Line.blank());
        out.add(Line.plain("Elapsed: " + p.getElapsedMs() + " ms", TEXT_DIM));
        return out;
    }

    private void drawBackground(Graphics2D g, int height) {
        g.setColor(BG);
        g.fillRect(0, 0, WIDTH, height);
    }

    private void drawTitleBar(Graphics2D g, String testName, HttpProbeResult p) {
        g.setColor(HEADER_BG);
        g.fillRect(0, 0, WIDTH, 38);

        Font titleFont = new Font(Font.MONOSPACED, Font.BOLD, 13);
        g.setFont(titleFont);
        g.setColor(Color.WHITE);
        g.drawString("OWASP Test Evidence  |  " + testName, PADDING, 24);
    }

    private void drawLine(Graphics2D g, Line line, int y) {
        Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 11);
        Font monoBold = new Font(Font.MONOSPACED, Font.BOLD, 11);

        if (line.isSection) {
            g.setFont(monoBold);
            g.setColor(new Color(100, 160, 220));
            g.drawString(line.key, PADDING, y + LINE_H - 3);
        } else if (line.isBlank) {
            // nothing
        } else if (line.hasKV) {
            g.setFont(mono);
            g.setColor(line.keyColor);
            g.drawString(truncate(line.key, 24), PADDING, y + LINE_H - 3);
            g.setColor(line.valColor);
            g.drawString(truncate(line.val, 95), PADDING + TAB_STOP, y + LINE_H - 3);
        } else {
            g.setFont(mono);
            g.setColor(line.keyColor);
            g.drawString(truncate(line.key, 120), PADDING, y + LINE_H - 3);
        }
    }

    private static void applyHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,       RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,  RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,          RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static String truncate(String s, int maxChars) {
        if (s == null) return "";
        return s.length() > maxChars ? s.substring(0, maxChars - 1) + "…" : s;
    }

    private static Color statusColor(int code) {
        if (code < 300) return STATUS_2XX;
        if (code < 400) return STATUS_3XX;
        if (code < 500) return STATUS_4XX;
        return STATUS_5XX;
    }

    // ─── Line record ─────────────────────────────────────────────────────────

    private static class Line {
        String key, val;
        Color  keyColor, valColor;
        boolean isSection, isBlank, hasKV;

        static Line section(String text) {
            Line l = new Line(); l.key = text; l.isSection = true; return l;
        }
        static Line blank() {
            Line l = new Line(); l.isBlank = true; return l;
        }
        static Line plain(String text, Color color) {
            Line l = new Line(); l.key = text; l.keyColor = color; return l;
        }
        static Line kv(String key, String val, Color kc, Color vc) {
            Line l = new Line(); l.key = key; l.val = val;
            l.keyColor = kc; l.valColor = vc; l.hasKV = true; return l;
        }
    }
}
