package io.github.hbvit7;

import java.awt.image.DataBufferInt;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import java.awt.image.BufferedImage;
import java.io.File;
import org.eclipse.swt.widgets.Spinner;

public class Main {

    private static final String TITLE = "PDF Viewer с панелью инструментов";
    private static Image swtImage = null; // Текущая страница PDF
    private static int currentPage;
    private static int maxPage;
    private static String filePath;

    private static int topSize;

    public static void main(String[] args) {
        Display display = new Display();
        Shell shell = new Shell(display);
        shell.setText(TITLE);
        shell.setSize(700, 900);

        // Главный макет окна — вертикальная сетка из 1 колонки
        shell.setLayout(new GridLayout(1, false));

        // =================================================================
        // ПОДЛОЖКА / КОНТЕЙНЕР ДЛЯ КНОПОК (ВЕРХНЯЯ ПАНЕЛЬ)
        // =================================================================
        Composite toolsContainer = new Composite(shell, SWT.NONE);
        toolsContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // RowLayout размещает элементы в одну строку горизонтально
        RowLayout toolbarLayout = new RowLayout(SWT.HORIZONTAL);
        toolbarLayout.marginTop = 5;
        toolbarLayout.marginBottom = 5;
        toolbarLayout.marginLeft = 5;
        toolbarLayout.spacing = 10; // Расстояние между кнопками в пикселях
        toolsContainer.setLayout(toolbarLayout);

        // Кнопка "Открыть", привязанная к нашей панели
        Button openBtn = new Button(toolsContainer, SWT.PUSH);
        openBtn.setText("Открыть PDF");

        Button prevBtn = new Button(toolsContainer, SWT.PUSH);
        prevBtn.setText("◀ Назад");

        Button nextBtn = new Button(toolsContainer, SWT.PUSH);
        nextBtn.setText("Вперед ▶");

        Spinner top = new Spinner(toolsContainer, SWT.BORDER);
        top.setMinimum(0);       // Минимальное значение
        top.setMaximum(50);     // Максимальное значение
        top.setSelection(0);    // Значение по умолчанию
        top.setIncrement(1);

        // =================================================================
        // ОБЛАСТЬ ОТРИСОВКИ (CANVAS)
        // =================================================================
        Canvas canvas = new Canvas(shell, SWT.BORDER | SWT.DOUBLE_BUFFERED);
        canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Слушатель отрисовки Canvas (с автоматическим масштабированием)
        canvas.addPaintListener(e -> {
            if (swtImage != null && !swtImage.isDisposed()) {
                Rectangle clientArea = canvas.getClientArea();
                Rectangle imgBounds = swtImage.getBounds();

                double scaleX = (double) clientArea.width / imgBounds.width;
                double scaleY = (double) clientArea.height / imgBounds.height;
                double scale = Math.min(scaleX, scaleY);

                int newWidth = (int) (imgBounds.width * scale);
                int newHeight = (int) (imgBounds.height * scale);

                int x = (clientArea.width - newWidth) / 2;
                int y = (clientArea.height - newHeight) / 2;

                e.gc.setAntialias(SWT.ON);
                e.gc.setInterpolation(SWT.HIGH);
                e.gc.drawImage(swtImage, 0, 0, imgBounds.width, imgBounds.height, x, y, newWidth, newHeight);
            } else {
                e.gc.drawString("Используйте панель инструментов сверху, чтобы открыть документ.", 20, 20);
            }
        });

        // Перерисовка при изменении размера окна
        canvas.addListener(SWT.Resize, event -> canvas.redraw());

        // Логика кнопки "Открыть"
        openBtn.addListener(SWT.Selection, event -> {
            FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
            fileDialog.setFilterExtensions(new String[]{"*.pdf"});

            filePath = fileDialog.open();
            currentPage = 0;

            displayPage(display, canvas, shell);
        });

        prevBtn.addListener(SWT.Selection, event -> {
            if (currentPage == 0) {
                return;
            }
            System.out.println("prev");
            currentPage--;
            displayPage(display, canvas, shell);
        });

        nextBtn.addListener(SWT.Selection, event -> {
            if (currentPage == maxPage) {
                return;
            }
            System.out.println("next");
            currentPage++;
            displayPage(display, canvas, shell);
        });

        top.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // Получаем актуальное числовое значение
                int currentValue = top.getSelection();
                System.out.println("Значение изменено (Top): " + currentValue);
            }
        });

        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }

        if (swtImage != null && !swtImage.isDisposed()) swtImage.dispose();
        display.dispose();
    }

    private static void displayPage(Display display, Canvas canvas, Shell shell) {
        if (filePath != null) {
            try {
                BufferedImage bufferedImage = renderPdfPage(filePath, currentPage);

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

        if (maxPage > 0) {
            shell.setText(TITLE + " " + (currentPage + 1) + " / " + (maxPage + 1));
        }
    }

    private static BufferedImage renderPdfPage(String filePath, int pageIndex) throws Exception {
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            maxPage = document.getNumberOfPages() - 1;
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            return pdfRenderer.renderImageWithDPI(pageIndex, 96);
        }
    }

    private static Image convertToSwtImage(Display display, BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        if (bufferedImage.getType() != BufferedImage.TYPE_INT_RGB) {
            BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            newImage.getGraphics().drawImage(bufferedImage, 0, 0, null);
            bufferedImage = newImage;
        }

        int[] data = ((DataBufferInt) bufferedImage.getRaster().getDataBuffer()).getData();
        PaletteData palette = new PaletteData(0xFF0000, 0xFF00, 0xFF);
        ImageData imageData = new ImageData(width, height, 32, palette);

        int[] swtPixels = new int[width];
        for (int y = 0; y < height; y++) {
            System.arraycopy(data, y * width, swtPixels, 0, width);
            imageData.setPixels(0, y, width, swtPixels, 0);
        }

        return new Image(display, imageData);
    }
}



