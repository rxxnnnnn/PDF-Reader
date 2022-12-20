package net.codebot.pdfviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Stack;

// PDF sample code from
// https://medium.com/@chahat.jain0/rendering-a-pdf-document-in-android-activity-fragment-using-pdfrenderer-442462cb8f9a
// Issues about cache etc. are not at all obvious from documentation, so we should expect people to need this.
// We may wish to provied them with this code.

public class MainActivity extends AppCompatActivity {

    final String LOGNAME = "pdf_viewer";
    final String FILENAME = "Shannon1948.pdf";
    final int FILERESID = R.raw.shannon1948;

    // manage the pages of the PDF, see below
    PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;
    private PdfRenderer.Page currentPage;
    private int totalpage;
    private int pagenumber;
    // custom ImageView class that captures strokes and draws them over the image
    ArrayList<PDFimage> pageImages;
    TextView pagenum;
    LinearLayout layout;
    Toolbar tools;
    TextView name;
    ImageView pen;
    ImageView highlight;
    ImageView eraser;
    ImageView pan;
    ImageView redo;
    ImageView undo;
    ImageView up;
    ImageView down;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOGNAME, "on Create");
        setContentView(R.layout.activity_main);
        layout = findViewById(R.id.pdfLayout);
        pagenum = findViewById(R.id.page);
        layout.setEnabled(true);
        tools = findViewById(R.id.toolbar);
        name = findViewById(R.id.name);
        pen = findViewById(R.id.pen);
        highlight = findViewById(R.id.highlight);
        eraser = findViewById(R.id.eraser);
        pan = findViewById(R.id.pan);
        redo = findViewById(R.id.redo);
        undo = findViewById(R.id.undo);
        up = findViewById(R.id.up);
        down = findViewById(R.id.down);

        name.setText(FILENAME);
        pen.setSelected(true);
        pen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pageImages.get(pagenumber).mode=0;
                pen.setSelected(true);
                highlight.setSelected(false);
                eraser.setSelected(false);
                pan.setSelected(false);
            }
        });

        highlight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pageImages.get(pagenumber).mode=1;
                Log.d(LOGNAME, "pen");
                pen.setSelected(false);
                highlight.setSelected(true);
                eraser.setSelected(false);
                pan.setSelected(false);
            }
        });

        eraser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pageImages.get(pagenumber).mode=2;
                pen.setSelected(false);
                highlight.setSelected(false);
                eraser.setSelected(true);
                pan.setSelected(false);
            }
        });

        pan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pageImages.get(pagenumber).mode=3;
                pen.setSelected(false);
                highlight.setSelected(false);
                eraser.setSelected(false);
                pan.setSelected(true);
            }
        });
        undo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pageImages.get(pagenumber).undo();
            }
        });

        redo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pageImages.get(pagenumber).redo();
            }
        });


        up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pagenumber > 0) {
                    layout.removeView(pageImages.get(pagenumber));
                    int mode = pageImages.get(pagenumber).mode;
                    pageImages.get(pagenumber).currentm.setScale(1, 1, pageImages.get(pagenumber).midpoint.x, pageImages.get(pagenumber).midpoint.y);
                    pagenumber--;
                    pageImages.get(pagenumber).mode = mode;
                    pagenum.setText("Page: " + (pagenumber + 1) + "/" + totalpage + " ");
                    showPage(pagenumber);
                }
            }
        });

        down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pagenumber < totalpage - 1) {
                    layout.removeView(pageImages.get(pagenumber));
                    int mode = pageImages.get(pagenumber).mode;
                    pageImages.get(pagenumber).currentm.setScale(1, 1, pageImages.get(pagenumber).midpoint.x, pageImages.get(pagenumber).midpoint.y);
                    pagenumber++;
                    pageImages.get(pagenumber).mode = mode;
                    pagenum.setText("Page: " + (pagenumber + 1) + "/" + totalpage + " ");
                    showPage(pagenumber);
                }
            }
        });



            // open page 0 of the PDF
            // it will be displayed as an image in the pageImage (above)
            try {
                if(pdfRenderer==null) {
                    openRenderer(this);
                }
                totalpage = pdfRenderer.getPageCount();
                pagenumber = 0;
                if(pageImages==null) {
                    Log.d(LOGNAME, "pageImages is null");
                    pageImages = new ArrayList<>();
                    for (int i = 0; i < totalpage; i++) {
                        PDFimage pageImage = new PDFimage(this);
                        pageImage.setMinimumWidth(1800);
                        pageImage.setMinimumHeight(2500);
                        pageImage.paths.clear();
                        pageImage.redos.clear();
                        pageImages.add(pageImage);
                    }
                    File file = new File(getFilesDir(), "appSaveState.data");
                    if (file.exists()) {
                        Log.d(LOGNAME, "exist PDF");
                        ObjectInputStream inFile = null;
                        try {
                            inFile = new ObjectInputStream(new FileInputStream(file));
                            for (int i = 0; i < totalpage; i++) {
                                pageImages.get(i).paths = (ArrayList<mPath>) inFile.readObject();
                                for (mPath path : pageImages.get(i).paths) {
                                    path.mpath.updatePath();
                                }
                            }
                            inFile.close();
                        } catch (IOException | ClassNotFoundException e) {
                            Log.d(LOGNAME, String.valueOf(e));
                            e.printStackTrace();
                        }
                    }
                }
                pagenum.setText("Page: " + (pagenumber + 1) + "/" + totalpage + "  ");
                showPage(pagenumber);
                //    closeRenderer();
            } catch (IOException exception) {
                Log.d(LOGNAME, "Error opening PDF");
            }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(LOGNAME, "onSaveInstanceState");
        File outFile = new File(getFilesDir(), "appSaveState.data");
        ObjectOutputStream outStream = null;
        try {
            outStream = new ObjectOutputStream(new FileOutputStream(outFile));
            for(int i = 0; i < pageImages.size();i++) {
                outStream.writeObject(pageImages.get(i).paths);
            }
            Log.d(LOGNAME, "save successful");
            outStream.close();
        } catch (IOException e) {
            Log.d(LOGNAME, String.valueOf(e));
            e.printStackTrace();
        }
        super.onSaveInstanceState(outState);
    }




    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onStop() {
        Log.d(LOGNAME, "onStop");
        super.onStop();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onDestroy() {
        Log.d(LOGNAME, "onDestroy");
        super.onDestroy();
        try {
            closeRenderer();
        } catch (IOException ex) {
            Log.d(LOGNAME, "Unable to close PDF renderer");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openRenderer(Context context) throws IOException {
        // In this sample, we read a PDF from the assets directory.
        File file = new File(context.getCacheDir(), FILENAME);
        if (!file.exists()) {
            // pdfRenderer cannot handle the resource directly,
            // so extract it into the local cache directory.
            InputStream asset = this.getResources().openRawResource(FILERESID);
            FileOutputStream output = new FileOutputStream(file);
            final byte[] buffer = new byte[1024];
            int size;
            while ((size = asset.read(buffer)) != -1) {
                output.write(buffer, 0, size);
            }
            asset.close();
            output.close();
        }
        parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE);

        // capture PDF data
        // all this just to get a handle to the actual PDF representation
        if (parcelFileDescriptor != null) {
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
        }
    }

    // do this before you quit!
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void closeRenderer() throws IOException {
        if (null != currentPage) {
            currentPage.close();
        }
        pdfRenderer.close();
        parcelFileDescriptor.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void showPage(int index) {
        if (pdfRenderer.getPageCount() <= index) {
            return;
        }
        // Close the current page before opening another one.
        if (null != currentPage) {
            currentPage.close();
        }
        // Use `openPage` to open a specific page in PDF.
        currentPage = pdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);

        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        LinearLayout layout = findViewById(R.id.pdfLayout);
        layout.addView(pageImages.get(pagenumber));
        TextView number = new TextView(this);
        number.setText("Page 1/5");
       // layout.addView();
        // Display the page
        pageImages.get(pagenumber).setImage(bitmap);

    }
}
