package io.github.hbvit7;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import java.awt.image.BufferedImage;
import java.awt.image.DirectColorModel;
import java.io.File;

public class Main {

    private static Image swtImage = null; // Хранит текущую отрендеренную страницу

    public static void main(String[] args) {
        Display display = new Display();
        Shell shell = new Shell(display);
        shell.setText("PDFBox SWT Canvas Viewer");
        shell.setSize(600, 800);
        shell.setLayout(new GridLayout(1, false));

        // Кнопка для выбора PDF
        Button openBtn = new Button(shell, SWT.PUSH);
        openBtn.setText("Открыть PDF файл");
        openBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Холст (Canvas) для отрисовки PDF
        Canvas canvas = new Canvas(shell, SWT.BORDER | SWT.DOUBLE_BUFFERED);
        canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Слушатель отрисовки холста
        canvas.addPaintListener(e -> {
            if (swtImage != null && !swtImage.isDisposed()) {
                // Рисуем изображение по центру холста или с верхнего левого угла
                e.gc.drawImage(swtImage, 0, 0);
            } else {
                e.gc.drawString("Изображение не загружено. Откройте PDF файл.", 20, 20);
            }
        });

        // Логика нажатия на кнопку
        openBtn.addListener(SWT.Selection, event -> {
            FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
            fileDialog.setFilterExtensions(new String[]{"*.pdf"});
            String filePath = fileDialog.open();

            if (filePath != null) {
                try {
                    // 1. Рендерим PDF страницу в BufferedImage с помощью PDFBox
                    BufferedImage bufferedImage = renderPdfPage(filePath, 0);

                    // 2. Конвертируем BufferedImage в SWT Image
                    if (swtImage != null) swtImage.dispose(); // Очищаем старую память
                    swtImage = convertToSwtImage(display, bufferedImage);

                    // 3. Обновляем холст
                    canvas.redraw();

                } catch (Exception e) {
                    MessageBox box = new MessageBox(shell, SWT.ICON_ERROR);
                    box.setMessage("Ошибка при чтении PDF: " + e.getMessage());
                    box.open();
                }
            }
        });

        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }

        // Чистим ресурсы SWT перед выходом
        if (swtImage != null && !swtImage.isDisposed()) swtImage.dispose();
        display.dispose();
    }

    /**
     * Рендерит указанную страницу PDF файла в BufferedImage (AWT)
     */
    private static BufferedImage renderPdfPage(String filePath, int pageIndex) throws Exception {
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            // 150 DPI обеспечивает хороший баланс между качеством и скоростью рендеринга
            return pdfRenderer.renderImageWithDPI(pageIndex, 150);
        }
    }

    /**
     * Конвертирует BufferedImage (AWT) в SWT Image
     */
    private static Image convertToSwtImage(Display display, BufferedImage bufferedImage) {
        DirectColorModel colorModel = (DirectColorModel) bufferedImage.getColorModel();
        PaletteData palette = new PaletteData(colorModel.getRedMask(), colorModel.getGreenMask(), colorModel.getBlueMask());

        ImageData data = new ImageData(
                bufferedImage.getWidth(),
                bufferedImage.getHeight(),
                colorModel.getPixelSize(),
                palette
        );

        // Попиксельно переносим данные RGB
        for (int y = 0; y < data.height; y++) {
            for (int x = 0; x < data.width; x++) {
                int rgb = bufferedImage.getRGB(x, y);
                int pixel = palette.getPixel(new org.eclipse.swt.graphics.RGB(
                        (rgb >> 16) & 0xFF,
                        (rgb >> 8) & 0xFF,
                        rgb & 0xFF
                ));
                data.setPixel(x, y, pixel);
            }
        }
        return new Image(display, data);
    }
}

