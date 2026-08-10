/*
* Copyleft © 2024-2026 L2Brproject
* Splash com logo nativo + nome BrProject com efeito neon.
*
* Desenvolvido por Dev ⩽ A.L.N/⩾ para o BrProject.
*/
package ext.mods.security.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.Timer;

/**
 * Load screen: logo nativo + titulo com glow/shimmer, barra 0→100%.
 * Desenvolvido por Dev ⩽ A.L.N/⩾ para o BrProject.
 */
public final class BootSplash extends JWindow
{
	private static final int WIDTH = 520;
	private static final int HEIGHT = 320;
	private static final int DOTS = 6;
	private static final int LOAD_MS = 2800;
	private static final int HOLD_AT_100_MS = 450;
	private static final int LOGO_SIZE = 64;

	private final Timer animator;
	private final Image logo;
	private float t;
	private float progress;
	private long startMs;
	private boolean finished;
	private String status = "Iniciando...";
	private Runnable onComplete;

	public BootSplash()
	{
		logo = loadLogo();
		setSize(WIDTH, HEIGHT);
		setLocationRelativeTo(null);
		setAlwaysOnTop(true);
		setBackground(new Color(0, 0, 0, 0));

		JPanel canvas = new JPanel()
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

				g2.setPaint(new LinearGradientPaint(
					0, 0, 0, getHeight(),
					new float[] { 0f, 1f },
					new Color[] { new Color(16, 12, 28), new Color(8, 8, 14) }));
				g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 32, 32));

				float cx = getWidth() / 2f;
				float cy = getHeight() / 2f + 10;
				g2.setPaint(new RadialGradientPaint(
					cx, cy, 170f,
					new float[] { 0f, 1f },
					new Color[] { new Color(120, 60, 220, 55), new Color(120, 60, 220, 0) },
					MultipleGradientPaint.CycleMethod.NO_CYCLE));
				g2.fill(new Ellipse2D.Float(cx - 170, cy - 170, 340, 340));

				float borderPulse = 0.55f + 0.45f * (float) Math.sin(t * 2.2);
				g2.setStroke(new BasicStroke(2.6f));
				g2.setColor(new Color(160, 90, 255, Math.round(140 + 90 * borderPulse)));
				g2.draw(new RoundRectangle2D.Float(2, 2, getWidth() - 4, getHeight() - 4, 30, 30));
				g2.setStroke(new BasicStroke(1.1f));
				g2.setColor(new Color(90, 210, 255, 90));
				g2.draw(new RoundRectangle2D.Float(7, 7, getWidth() - 14, getHeight() - 14, 24, 24));

				paintTitleWithLogo(g2);

				g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
				String tag = "Projeto Exclusivo BrProject";
				int tagW = g2.getFontMetrics().stringWidth(tag);
				g2.setColor(new Color(120, 200, 230));
				g2.drawString(tag, (getWidth() - tagW) / 2, 118);

				paintSignature(g2);

				float orbitR = 38f;
				float ox = cx;
				float oy = 188f;
				g2.setStroke(new BasicStroke(1.4f));
				g2.setColor(new Color(140, 80, 255, 50));
				g2.draw(new Ellipse2D.Float(ox - orbitR, oy - orbitR, orbitR * 2, orbitR * 2));

				for (int i = 0; i < DOTS; i++)
				{
					double ang = t * 2.4 + i * (Math.PI * 2.0 / DOTS);
					float px = ox + (float) Math.cos(ang) * orbitR;
					float py = oy + (float) Math.sin(ang) * (orbitR * 0.55f);
					float wave = (float) ((Math.sin(t * 3.0 + i) + 1.0) * 0.5);
					float r = 5.5f + wave * 5.5f;

					g2.setColor(new Color(150, 70, 255, 70));
					g2.fill(new Ellipse2D.Float(px - r - 4, py - r - 4, (r + 4) * 2, (r + 4) * 2));

					Color core = (i % 2 == 0)
						? new Color(180, 120, 255, 220)
						: new Color(80, 210, 240, 230);
					g2.setColor(core);
					g2.fill(new Ellipse2D.Float(px - r, py - r, r * 2, r * 2));
				}

				int pct = Math.round(progress * 100f);
				String line = status + "  " + pct + "%";
				g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
				int sw = g2.getFontMetrics().stringWidth(line);
				g2.setColor(new Color(180, 170, 200));
				g2.drawString(line, (getWidth() - sw) / 2, 258);

				int barW = 280;
				int barX = (getWidth() - barW) / 2;
				int barY = 270;
				g2.setColor(new Color(40, 30, 55));
				g2.fill(new RoundRectangle2D.Float(barX, barY, barW, 10, 10, 10));

				int fill = Math.max(0, Math.round(barW * Math.min(1f, progress)));
				if (fill > 0)
				{
					g2.setPaint(new LinearGradientPaint(
						barX, 0, barX + Math.max(fill, 1), 0,
						new float[] { 0f, 1f },
						new Color[] { new Color(90, 40, 200), new Color(80, 200, 230) }));
					g2.fill(new RoundRectangle2D.Float(barX, barY, fill, 10, 10, 10));
				}

				g2.dispose();
			}
		};
		canvas.setOpaque(false);
		canvas.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setContentPane(canvas);
		pack();
		setShape(new RoundRectangle2D.Float(0, 0, WIDTH, HEIGHT, 32, 32));

		animator = new Timer(16, e -> tick());
	}

	private void paintTitleWithLogo(Graphics2D g2)
	{
		final String title = "BR PROJECT";
		g2.setFont(new Font("Segoe UI", Font.BOLD, 36));
		FontMetrics fm = g2.getFontMetrics();
		int tw = fm.stringWidth(title);
		int gap = 14;
		int blockW = (logo != null ? LOGO_SIZE + gap : 0) + tw;
		int startX = (getWidth() - blockW) / 2;
		int titleBaseline = 82;
		int logoY = titleBaseline - LOGO_SIZE + 8;

		if (logo != null)
		{
			// Logo nitido, sem glow por cima (evita aspecto ofuscado)
			int lx = startX;
			int ly = logoY;
			g2.drawImage(logo, lx, ly, LOGO_SIZE, LOGO_SIZE, null);
			startX += LOGO_SIZE + gap;
		}

		// Sombra suave atras do texto (nao no logo)
		g2.setColor(new Color(0, 0, 0, 90));
		g2.drawString(title, startX + 2, titleBaseline + 2);

		// Gradiente animado roxo → ciano no nome
		float shift = (float) ((Math.sin(t * 1.4) + 1.0) * 0.5);
		float x1 = startX + tw * (shift - 0.35f);
		float x2 = startX + tw * (shift + 0.65f);
		if (Math.abs(x2 - x1) < 8f)
			x2 = x1 + 8f;
		g2.setPaint(new LinearGradientPaint(
			x1, 0, x2, 0,
			new float[] { 0f, 0.5f, 1f },
			new Color[] {
				new Color(170, 90, 255),
				new Color(245, 235, 255),
				new Color(70, 210, 245)
			}));
		g2.drawString(title, startX, titleBaseline);

		// Brilho passando so no texto
		float shineX = startX + (tw + 40) * ((t * 0.35f) % 1.2f) - 20;
		g2.setPaint(new LinearGradientPaint(
			shineX, 0, shineX + 36, 0,
			new float[] { 0f, 0.5f, 1f },
			new Color[] {
				new Color(255, 255, 255, 0),
				new Color(255, 255, 255, 110),
				new Color(255, 255, 255, 0)
			}));
		g2.drawString(title, startX, titleBaseline);
	}

	/** Assinatura: Dev ⩽ A.L.N/⩾ — nome em destaque */
	private void paintSignature(Graphics2D g2)
	{
		final String prefix = "Dev ⩽ ";
		final String name = "A.L.N";
		final String suffix = "/⩾";

		Font prefixFont = pickSigFont(new Font("Segoe UI", Font.PLAIN, 12), prefix + suffix);
		Font nameFont = pickSigFont(new Font("Segoe UI Semibold", Font.BOLD, 18), name);
		if (nameFont.getStyle() != Font.BOLD)
			nameFont = pickSigFont(new Font("Dialog", Font.BOLD, 18), name);

		FontMetrics fmP = g2.getFontMetrics(prefixFont);
		FontMetrics fmN = g2.getFontMetrics(nameFont);
		int sw = fmP.stringWidth(prefix) + fmN.stringWidth(name) + fmP.stringWidth(suffix);
		int x = (getWidth() - sw) / 2;
		int y = 144;

		// Linha decorativa acima
		float lineW = Math.max(sw + 24f, 140f);
		float lx = (getWidth() - lineW) / 2f;
		g2.setStroke(new BasicStroke(1.2f));
		g2.setPaint(new LinearGradientPaint(
			lx, 0, lx + lineW, 0,
			new float[] { 0f, 0.5f, 1f },
			new Color[] {
				new Color(140, 80, 255, 0),
				new Color(200, 170, 255, 180),
				new Color(80, 210, 240, 0)
			}));
		g2.drawLine(Math.round(lx), y - 20, Math.round(lx + lineW), y - 20);

		float alphaPulse = 0.82f + 0.18f * (float) Math.sin(t * 2.0);
		g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alphaPulse));

		int cx = x;
		// "Dev ⩽ " — discreto
		g2.setFont(prefixFont);
		g2.setColor(new Color(0, 0, 0, 110));
		g2.drawString(prefix, cx + 1, y + 1);
		g2.setColor(new Color(190, 165, 240, 210));
		g2.drawString(prefix, cx, y);
		cx += fmP.stringWidth(prefix);

		// "A.L.N" — bem destacado
		float shift = (float) ((Math.sin(t * 1.6) + 1.0) * 0.5);
		float gx1 = cx + fmN.stringWidth(name) * (shift - 0.35f);
		float gx2 = cx + fmN.stringWidth(name) * (shift + 0.75f);
		if (Math.abs(gx2 - gx1) < 12f)
			gx2 = gx1 + 12f;

		g2.setFont(nameFont);
		g2.setColor(new Color(0, 0, 0, 140));
		g2.drawString(name, cx + 2, y + 2);
		g2.setPaint(new LinearGradientPaint(
			gx1, 0, gx2, 0,
			new float[] { 0f, 0.4f, 1f },
			new Color[] {
				new Color(210, 170, 255),
				new Color(255, 255, 255),
				new Color(120, 230, 255)
			}));
		g2.drawString(name, cx, y);

		// Brilho so no nome
		float shineX = cx + (fmN.stringWidth(name) + 40) * ((t * 0.32f) % 1.25f) - 20;
		g2.setPaint(new LinearGradientPaint(
			shineX, 0, shineX + 22, 0,
			new float[] { 0f, 0.5f, 1f },
			new Color[] {
				new Color(255, 255, 255, 0),
				new Color(255, 255, 255, 200),
				new Color(255, 255, 255, 0)
			}));
		g2.drawString(name, cx, y);
		cx += fmN.stringWidth(name);

		// "/⩾"
		g2.setFont(prefixFont);
		g2.setColor(new Color(0, 0, 0, 110));
		g2.drawString(suffix, cx + 1, y + 1);
		g2.setColor(new Color(190, 165, 240, 210));
		g2.drawString(suffix, cx, y);

		g2.setComposite(java.awt.AlphaComposite.SrcOver);
	}

	private static Font pickSigFont(Font preferred, String sample)
	{
		if (preferred.canDisplayUpTo(sample) == -1)
			return preferred;
		Font alt = new Font("Segoe UI", preferred.getStyle(), preferred.getSize());
		if (alt.canDisplayUpTo(sample) == -1)
			return alt;
		return new Font("Dialog", preferred.getStyle(), preferred.getSize());
	}

	private static Image loadLogo()
	{
		String[] paths = {
			"images/brproject-gear-crisp.png",
			"../images/brproject-gear-crisp.png",
			"images/brproject-gear-128.png",
			"images/brproject-gear.png",
			"images/32x32.png",
			"images/start-icon.png"
		};
		for (String p : paths)
		{
			try
			{
				File f = new File(p);
				if (!f.isFile())
					continue;
				Image img = ImageIO.read(f);
				if (img != null)
					return img;
			}
			catch (Exception ignored) {}
		}
		return null;
	}

	private void tick()
	{
		t += 0.045f;
		long elapsed = System.currentTimeMillis() - startMs;

		if (elapsed < LOAD_MS)
		{
			float p = elapsed / (float) LOAD_MS;
			progress = 1f - (1f - p) * (1f - p) * (1f - p);

			if (progress < 0.25f)
				status = "Preparando ambiente...";
			else if (progress < 0.55f)
				status = "Carregando interface...";
			else if (progress < 0.85f)
				status = "Montando painel...";
			else
				status = "Quase pronto...";
		}
		else
		{
			progress = 1f;
			status = "Concluido";
			if (!finished && elapsed >= LOAD_MS + HOLD_AT_100_MS)
			{
				finished = true;
				animator.stop();
				if (onComplete != null)
					onComplete.run();
			}
		}
		repaint();
	}

	public void openAndWait(Runnable whenReady)
	{
		this.onComplete = whenReady;
		this.startMs = System.currentTimeMillis();
		this.progress = 0f;
		this.finished = false;
		animator.start();
		setVisible(true);
		toFront();
	}

	public void closeSplash()
	{
		animator.stop();
		setVisible(false);
		dispose();
	}
}
