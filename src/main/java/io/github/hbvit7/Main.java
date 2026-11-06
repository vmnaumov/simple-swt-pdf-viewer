package io.github.hbvit7;

import java.awt.image.DataBufferInt;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Rectangle;
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

    private static Image swtImage = null; // Текущая страница PDF в виде изображения SWT

    public static void main(String[] args) {
        Display display = new Display();
        Shell shell = new Shell(display);
        shell.setText("PDFBox SWT Canvas: Fit to Window");
        shell.setSize(600, 800);
        shell.setLayout(new GridLayout(1, false));

        Button openBtn = new Button(shell, SWT.PUSH);
        openBtn.setText("Открыть PDF файл");
        openBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Обязательно используем SWT.DOUBLE_BUFFERED, чтобы избежать мерцания при изменении размеров окна
        Canvas canvas = new Canvas(shell, SWT.BORDER | SWT.DOUBLE_BUFFERED);
        canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Слушатель отрисовки холста
        canvas.addPaintListener(e -> {
            if (swtImage != null && !swtImage.isDisposed()) {
                Rectangle clientArea = canvas.getClientArea();
                Rectangle imgBounds = swtImage.getBounds();

                // 1. Вычисляем коэффициенты масштабирования по ширине и высоте
                double scaleX = (double) clientArea.width / imgBounds.width;
                double scaleY = (double) clientArea.height / imgBounds.height;

                // 2. Выбираем минимальный коэффициент, чтобы страница поместилась целиком
                double scale = Math.min(scaleX, scaleY);

                // 3. Вычисляем итоговые размеры отмасштабированного изображения
                int newWidth = (int) (imgBounds.width * scale);
                int newHeight = (int) (imgBounds.height * scale);

                // 4. Центрируем изображение на холсте
                int x = (clientArea.width - newWidth) / 2;
                int y = (clientArea.height - newHeight) / 2;

                // Включаем продвинутое сглаживание для красивого масштабирования текста
                e.gc.setAntialias(SWT.ON);
                e.gc.setInterpolation(SWT.HIGH);

                // Отрисовываем картинку с масштабированием: drawImage(image, srcX, srcY, srcW, srcH, destX, destY, destW, destH)
                e.gc.drawImage(swtImage, 0, 0, imgBounds.width, imgBounds.height, x, y, newWidth, newHeight);
            } else {
                e.gc.drawString("Откройте PDF файл для отображения.", 20, 20);
            }
        });

        // Перерисовываем холст при каждом изменении размеров окна
        canvas.addListener(SWT.Resize, event -> canvas.redraw());

        openBtn.addListener(SWT.Selection, event -> {
            FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
            fileDialog.setFilterExtensions(new String[]{"*.pdf"});
            String filePath = fileDialog.open();

            if (filePath != null) {
                try {
                    // Рендерим с базовым качеством (DPI=72), так как масштабирование сделает SWT
                    BufferedImage bufferedImage = renderPdfPage(filePath, 0);

                    if (swtImage != null) swtImage.dispose();
                    swtImage = convertToSwtImage(display, bufferedImage);

                    canvas.redraw();
                } catch (Exception e) {
                    MessageBox box = new MessageBox(shell, SWT.ICON_ERROR);
                    box.setMessage("Ошибка при чтении PDF: " + e.getMessage());
                    box.open();
                    e.printStackTrace();
                }
            }
        });

        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }

        if (swtImage != null && !swtImage.isDisposed()) swtImage.dispose();
        display.dispose();
    }

    private static BufferedImage renderPdfPage(String filePath, int pageIndex) throws Exception {
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            // 72 или 96 DPI достаточно, так как SWT растянет картинку аппаратно
            return pdfRenderer.renderImageWithDPI(pageIndex, 96);
        }
    }

    /**
     * Оптимизированная конвертация BufferedImage в SWT Image через побитовый сдвиг
     */
    private static Image convertToSwtImage(Display display, BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        // Переводим изображение в стандартный формат INT_RGB, если оно было в другом
        if (bufferedImage.getType() != BufferedImage.TYPE_INT_RGB) {
            BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            newImage.getGraphics().drawImage(bufferedImage, 0, 0, null);
            bufferedImage = newImage;
        }

        int[] data = ((DataBufferInt) bufferedImage.getRaster().getDataBuffer()).getData();

        // Маски для формата INT_RGB в AWT
        PaletteData palette = new PaletteData(0xFF0000, 0xFF00, 0xFF);
        ImageData imageData = new ImageData(width, height, 32, palette);

        // Быстрый перенос массива пикселей
        int[] swtPixels = new int[width];
        for (int y = 0; y < height; y++) {
            System.arraycopy(data, y * width, swtPixels, 0, width);
            imageData.setPixels(0, y, width, swtPixels, 0);
        }

        return new Image(display, imageData);
    }
}


