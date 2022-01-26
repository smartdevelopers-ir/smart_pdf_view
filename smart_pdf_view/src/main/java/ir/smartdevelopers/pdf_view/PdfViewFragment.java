package ir.smartdevelopers.pdf_view;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContentResolverCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Link;
import com.artifex.mupdf.fitz.Matrix;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.Quad;
import com.artifex.mupdf.fitz.SeekableInputStream;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;

import java.io.IOException;

public class PdfViewFragment extends Fragment implements ActionListener {
    private ViewPager2 mViewPager2;
    private PdfAdapter mAdapter;
    protected float displayDPI;
    protected float pageZoom;
    protected int pageCount;
    protected int mCurrentPage;
    protected int canvasW, canvasH;
    protected float layoutW, layoutH, layoutEm;
    protected Document doc;
    protected SeekableInputStream file;

    protected boolean hasLoaded;
    protected boolean isReflowable;
    protected boolean fitPage;
    protected Worker worker;
    private String mMimeType;
    protected boolean wentBack;
    private ProgressBar mProgressBar;

    public static PdfViewFragment getInstance(Uri uri) {
        Bundle bundle=new Bundle();
        bundle.putParcelable("uri",uri);
        PdfViewFragment fragment=new PdfViewFragment();
        fragment.setArguments(bundle);
        return fragment;
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pdf_view,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        displayDPI = getResources().getDisplayMetrics().densityDpi;
        hasLoaded = false;
        Uri uri=null;
        Bundle bundle=getArguments();
        if (bundle != null) {
            uri = bundle.getParcelable("uri");
        }

        Cursor cursor = ContentResolverCompat.query(view.getContext().getContentResolver(), uri, null, null, null,null,null);
        cursor.moveToFirst();
        int sizeIndex=cursor.getColumnIndex(OpenableColumns.SIZE);
        long size = cursor.getLong(sizeIndex);
        if (size == 0)
            size = -1;
        if (mMimeType == null || mMimeType.equals("application/octet-stream")) {
            mMimeType = view.getContext().getContentResolver().getType(uri);
        }

        try {
            file = new SeekableInputStreamWrapper(view.getContext().getContentResolver().openInputStream(uri), size);
        } catch (IOException x) {
            Log.e("TTT", x.toString());
            Toast.makeText(getContext(), x.getMessage(), Toast.LENGTH_SHORT).show();
        }
        worker = new Worker(getActivity());
        worker.start();
        mViewPager2=view.findViewById(R.id.viewPager);
        mProgressBar=view.findViewById(R.id.loading);
        mAdapter=new PdfAdapter(this,1);
        mViewPager2.setAdapter(mAdapter);

        openDocument();
    }
    public void onPageViewSizeChanged(int w, int h) {
        pageZoom = 1;
        canvasW = w;
        canvasH = h;
        layoutW = canvasW * 72 / displayDPI;
        layoutH = canvasH * 72 / displayDPI;

    }

    @Override
    public void gotoURI(String uri) {

    }

    @Override
    public void gotoPage(String uri) {

    }

    @Override
    public void toggleUI() {

    }

    public void onPageViewZoomChanged(float zoom) {
        if (zoom != pageZoom) {
            pageZoom = zoom;
            loadPage(mViewPager2.getCurrentItem());
        }
    }

    @Override
    public void goBackward() {

    }

    @Override
    public void goForward() {

    }

    protected void openDocument() {
        worker.add(new Worker.Task() {
            public void work() {
                Log.i("TTT", "open document");
                doc = Document.openDocument(file, mMimeType);
            }
            public void run() {
                loadDocument();
            }
        });
    }
    protected void loadDocument() {
        worker.add(new Worker.Task() {
            public void work() {
                try {

                    isReflowable = doc.isReflowable();
                    if (isReflowable) {
                        doc.layout(layoutW, layoutH, layoutEm);
                    }
                    pageCount = doc.countPages();
                } catch (Throwable x) {
                    doc = null;
                    pageCount = 1;
                    mCurrentPage = 0;
                    throw x;
                }
            }
            public void run() {
                if (mCurrentPage < 0 || mCurrentPage >= pageCount)
                    mCurrentPage = 0;
                mAdapter.setPageCount(pageCount);

            }
        });
    }

    protected void relayoutDocument() {
        worker.add(new Worker.Task() {
            public void work() {
                try {
                    long mark = doc.makeBookmark(doc.locationFromPageNumber(mCurrentPage));
                    Log.i("TTT", "relayout document");
                    doc.layout(layoutW, layoutH, layoutEm);
                    pageCount = doc.countPages();
                    mCurrentPage = doc.pageNumberFromLocation(doc.findBookmark(mark));
                } catch (Throwable x) {
                    pageCount = 1;
                    mCurrentPage = 0;
                    throw x;
                }
            }
            public void run() {
                loadPage(mViewPager2.getCurrentItem());
                //loadOutline();
            }
        });
    }
    protected void loadPage(int pageNumber) {
        //mProgressBar.setVisibility(View.VISIBLE);
        final float zoom = pageZoom;
        worker.add(new Worker.Task() {
            public Bitmap bitmap;
            public Link[] links;
            public Quad[] hits;
            public void work() {
                try {
                    Log.i("TTT", "load page " + pageNumber);
                    Page page = doc.loadPage(pageNumber);
                    Log.i("TTT", "draw page " + pageNumber + " zoom=" + zoom);
                    Matrix ctm;
                    if (fitPage)
                        ctm = AndroidDrawDevice.fitPage(page, canvasW, canvasH);
                    else
                        ctm = AndroidDrawDevice.fitPageWidth(page, canvasW);
                    links = page.getLinks();
                    if (links != null)
                        for (Link link : links)
                            link.bounds.transform(ctm);
                    if (zoom != 1)
                        ctm.scale(zoom);
                    bitmap = AndroidDrawDevice.drawPage(page, ctm);
                } catch (Throwable x) {
                    Log.e("TTT", x.getMessage());
                }
            }
            public void run() {
                if (bitmap != null) {
                    mAdapter.setBitmap(pageNumber, bitmap, zoom, wentBack, links, hits);
                    mProgressBar.setVisibility(View.GONE);
                }
                // pageLabel.setText((currentPage+1) + " / " + pageCount);
                // pageSeekbar.setMax(pageCount - 1);
                //pageSeekbar.setProgress(pageNumber);
                wentBack = false;


            }
        });
    }
}
