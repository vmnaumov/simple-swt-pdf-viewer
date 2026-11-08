package io.github.hbvit7;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.DataBufferInt;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.GestureEvent;
import org.eclipse.swt.events.GestureListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseListener;
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
    private static int rightSize;
    private static int bottomSize;
    private static int leftSize;

    private static BufferedImage rawPdfImage = null; // Исходный целый растр страницы PDF

    private static boolean isTopPage = true;

    // Переменная для отслеживания угла поворота (0, 90, 180, 270)
    private static int rotationDegrees = 0;

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

        Spinner topBottom = new Spinner(toolsContainer, SWT.BORDER);
        topBottom.setMinimum(0);       // Минимальное значение
        topBottom.setMaximum(50);     // Максимальное значение
        topBottom.setSelection(0);    // Значение по умолчанию
        topBottom.setIncrement(1);

        Spinner leftRight = new Spinner(toolsContainer, SWT.BORDER);
        leftRight.setMinimum(0);       // Минимальное значение
        leftRight.setMaximum(50);     // Максимальное значение
        leftRight.setSelection(0);    // Значение по умолчанию
        leftRight.setIncrement(1);

        Button closeBtn = new Button(toolsContainer, SWT.PUSH);
        closeBtn.setText("Закрыть");

        Button rotate = new Button(toolsContainer, SWT.PUSH);
        rotate.setText("Повернуть");
        rotate.setEnabled(false); // Отключена, пока не загружен файл

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

//                double scaleX = (double) clientArea.width / imgBounds.width;
//                double scaleY = (double) clientArea.height / imgBounds.height;
//                double scale = Math.min(scaleX, scaleY);
//
//                int newWidth = (int) (imgBounds.width * scale);
//                int newHeight = (int) (imgBounds.height * scale);
//
//                int x = (clientArea.width - newWidth) / 2;
//                int y = (clientArea.height - newHeight) / 2;

                e.gc.setAntialias(SWT.ON);
                e.gc.setInterpolation(SWT.HIGH);
//                e.gc.drawImage(swtImage, 0, 0, imgBounds.width, imgBounds.height, x, y, newWidth, newHeight);

                // Отрисовка в режиме "Заливка/Растяжение":
                // Берем картинку (от 0,0 до imgBounds.width/height)
                // и жестко вписываем в Canvas (от 0,0 до clientArea.width/height)
                if (rotationDegrees % 180 == 0) {
                    e.gc.drawImage(swtImage,
                            0, 0, imgBounds.width, imgBounds.height, // Откуда берем (исходник)
                            0, 0, clientArea.width, clientArea.height // Куда рисуем (весь Canvas)
                    );
                } else if (rotationDegrees == 90) {
                    if (isTopPage) {
                        e.gc.drawImage(swtImage,
                                imgBounds.width / 2, 0, imgBounds.width / 2 - 1,
                                imgBounds.height, // Откуда берем (исходник)
                                0, 0, clientArea.width, clientArea.height // Куда рисуем (весь Canvas)
                        );
                    } else {
                        e.gc.drawImage(swtImage,
                                0, 0, imgBounds.width / 2, imgBounds.height, // Откуда берем (исходник)
                                0, 0, clientArea.width, clientArea.height // Куда рисуем (весь Canvas)
                        );
                    }
                } else if (rotationDegrees == 270) {
                    if (isTopPage) {
                        e.gc.drawImage(swtImage,
                                0, 0, imgBounds.width / 2, imgBounds.height, // Откуда берем (исходник)
                                0, 0, clientArea.width, clientArea.height // Куда рисуем (весь Canvas)
                        );
                    } else {
                        e.gc.drawImage(swtImage,
                                imgBounds.width / 2, 0, imgBounds.width / 2 - 1,
                                imgBounds.height, // Откуда берем (исходник)
                                0, 0, clientArea.width, clientArea.height // Куда рисуем (весь Canvas)
                        );
                    }
                }
            } else {
                e.gc.drawString("Используйте панель инструментов сверху, чтобы открыть документ.", 20, 20);
            }
        });

        canvas.addGestureListener(new GestureListener() {
            @Override
            public void gesture(GestureEvent e) {
                System.out.println(e.detail);
                // Проверяем, что жест является свайпом
                if (e.detail == SWT.GESTURE_SWIPE) {
                    // Определяем направление свайпа с помощью полей x и y
                    // x = 1 (вправо), x = -1 (влево)
                    // y = 1 (вниз), y = -1 (вверх)

                    if (e.x != 0) {
                        if (e.x > 0) {
                            System.out.println("Свайп вправо (Swipe Right)");
                            prevPage(display, canvas, shell);
                        } else {
                            System.out.println("Свайп влево (Swipe Left)");
                            nextPage(display, canvas, shell);
                        }
                    }
                }
            }
        });

        canvas.addMouseListener(MouseListener.mouseUpAdapter(e -> {
            System.out.println("Кнопка нажата!");
            System.out.println(canvas.getSize().x);
            if (e.x < canvas.getSize().x / 2) {
                prevPage(display, canvas, shell);
            } else {
                nextPage(display, canvas, shell);
            }
        }));

        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.ARROW_RIGHT) {
                    System.out.println("Нажата стрелка ВПРАВО");
                    nextPage(display, canvas, shell);
                }

                if (e.keyCode == SWT.ARROW_LEFT) {
                    System.out.println("Нажата стрелка ВЛЕВО");
                    prevPage(display, canvas, shell);
                }
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
            rotate.setEnabled(true); // Активируем кнопку поворота
        });

        prevBtn.addListener(SWT.Selection, event -> {
            prevPage(display, canvas, shell);
        });

        nextBtn.addListener(SWT.Selection, event -> {
            nextPage(display, canvas, shell);
        });

        topBottom.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // Получаем актуальное числовое значение
                int currentValue = topBottom.getSelection();
                System.out.println("Значение изменено (Top-Bottom): " + currentValue);

                topSize = currentValue;
                bottomSize = currentValue;

                displayPage(display, canvas, shell);
            }
        });

        leftRight.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // Получаем актуальное числовое значение
                int currentValue = leftRight.getSelection();
                System.out.println("Значение изменено (Left-Right): " + currentValue);

                rightSize = currentValue;
                leftSize = currentValue;

                displayPage(display, canvas, shell);
            }
        });

        closeBtn.addListener(SWT.Selection, event -> {
            swtImage = null;
            canvas.redraw();
            rotate.setEnabled(false); // Дезактивируем кнопку поворота
        });

        rotate.addListener(SWT.Selection, event -> {
            if (swtImage != null) {
                // Увеличиваем угол на 90 градусов (цикл в пределах 0-270)
                rotationDegrees = (rotationDegrees + 90) % 360;

                // Пересоздаем изображение с новым поворотом и старыми процентами обрезки
                if (swtImage != null) swtImage.dispose();

                try {
                    rawPdfImage = renderPdfPage(filePath, currentPage);
                } catch (Exception e) {
                    MessageBox box = new MessageBox(shell, SWT.ICON_ERROR);
                    box.setMessage("Ошибка при чтении PDF: " + e.getMessage());
                    box.open();
                    e.printStackTrace();
                }

                swtImage = convertToSwtImage(display, rawPdfImage);

                // Обновляем холст
                canvas.redraw();
            }
        });

        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }

        if (swtImage != null && !swtImage.isDisposed()) swtImage.dispose();
        display.dispose();
    }

    private static void nextPage(Display display, Canvas canvas, Shell shell) {
        if (currentPage == maxPage) {
            return;
        }
        System.out.println("next");
        if (rotationDegrees == 90 || rotationDegrees == 270) {
            if (isTopPage) {
                isTopPage = false;
            } else {
                isTopPage = true;
                currentPage++;
            }
        } else {
            currentPage++;
        }
        displayPage(display, canvas, shell);
    }

    private static void prevPage(Display display, Canvas canvas, Shell shell) {
        if (currentPage == 0) {
            return;
        }
        System.out.println("prev");
        currentPage--;
        displayPage(display, canvas, shell);
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

    private static Image convertToSwtImage1(Display display, BufferedImage bufferedImage) {
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

    /**
     * Конвертирует BufferedImage в SWT Image с одновременным отсечением полей, заданных в процентах.
     */
    private static Image convertToSwtImage(Display display, BufferedImage bufferedImage) {

        System.out.println("topSize " + topSize);
        System.out.println("rightSize " + rightSize);
        System.out.println("bottomSize " + bottomSize);
        System.out.println("leftSize " + leftSize);

// 1. Поворот исходного BufferedImage средствами Java 2D
        BufferedImage rotatedImage = bufferedImage;
        if (rotationDegrees != 0) {
            int w = bufferedImage.getWidth();
            int h = bufferedImage.getHeight();

            // Определяем новые размеры картинки в зависимости от угла
            int newW = (rotationDegrees == 90 || rotationDegrees == 270) ? h : w;
            int newH = (rotationDegrees == 90 || rotationDegrees == 270) ? w : h;

            rotatedImage = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);

            Graphics2D g2d = rotatedImage.createGraphics();

            // Вычисляем трансформацию вокруг центра
            AffineTransform at = new AffineTransform();
            at.translate((newW - w) / 2.0, (newH - h) / 2.0);
            at.rotate(Math.toRadians(rotationDegrees), w / 2.0, h / 2.0);

            g2d.drawRenderedImage(bufferedImage, at);
            g2d.dispose();
        }

        // 2. Работаем с уже повернутым изображением для расчета обрезки
        int rotatedW = rotatedImage.getWidth();
        int rotatedH = rotatedImage.getHeight();

        int startX;
        int startY;
        int endX;
        int endY;

        if (rotationDegrees % 180 == 0) {
            startX = (int) Math.round(rotatedW * (leftSize / 100.0));
            startY = (int) Math.round(rotatedH * (topSize / 100.0));
            endX = rotatedW - (int) Math.round(rotatedW * (rightSize / 100.0));
            endY = rotatedH - (int) Math.round(rotatedH * (bottomSize / 100.0));
        } else {
            startX = (int) Math.round(rotatedW * (topSize / 100.0));
            startY = (int) Math.round(rotatedH * (leftSize / 100.0));
            endX = rotatedW - (int) Math.round(rotatedW * (bottomSize / 100.0));
            endY = rotatedH - (int) Math.round(rotatedH * (rightSize / 100.0));
        }

        int targetWidth = endX - startX;
        int targetHeight = endY - startY;

        if (targetWidth <= 0 || targetHeight <= 0) {
            return new Image(display, new ImageData(1, 1, 32, new PaletteData(0xFF0000, 0xFF00, 0xFF)));
        }

        // 3. Перенос пикселей в SWT ImageData
        int[] data = ((DataBufferInt) rotatedImage.getRaster().getDataBuffer()).getData();
        PaletteData palette = new PaletteData(0xFF0000, 0xFF00, 0xFF);
        ImageData imageData = new ImageData(targetWidth, targetHeight, 32, palette);

        int[] swtPixels = new int[targetWidth];
        for (int y = 0; y < targetHeight; y++) {
            int srcPos = (startY + y) * rotatedW + startX;
            System.arraycopy(data, srcPos, swtPixels, 0, targetWidth);
            imageData.setPixels(0, y, targetWidth, swtPixels, 0);
        }

        return new Image(display, imageData);
    }

}



