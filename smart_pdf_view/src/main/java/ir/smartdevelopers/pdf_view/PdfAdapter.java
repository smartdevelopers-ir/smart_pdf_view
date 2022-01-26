package ir.smartdevelopers.pdf_view;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.artifex.mupdf.fitz.Link;
import com.artifex.mupdf.fitz.Quad;

import java.util.List;
import java.util.Objects;

public class PdfAdapter extends RecyclerView.Adapter<PdfAdapter.PdfViewHolder> {
    private int mPageCount;
    private PdfViewFragment mParent;
    public PdfAdapter(PdfViewFragment parent, int pageCount) {
        mParent=parent;
        mPageCount=pageCount;
    }

    @NonNull
    @Override
    public PdfViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pdf_page,parent,false);
        return new PdfViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PdfViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.size()>0){
            for (Object obj:payloads){
                if (obj instanceof PdfDetail){
                    PdfDetail detail= (PdfDetail) obj;
                    holder.mPageView.setBitmap(detail.bitmap,detail.zoom,
                            detail.wentBack,detail.links,detail.hits);
                }else if (Objects.equals("error",obj)){
                    holder.mPageView.setError();
                }
            }
        }else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull PdfViewHolder holder, int position) {
        holder.mPageView.setActionListener(mParent);
        mParent.loadPage(position);
    }

    @Override
    public int getItemCount() {
        return mPageCount;
    }

    public void setPageCount(int pageCount) {

        if (pageCount > mPageCount){
            int diff = Math.abs(pageCount-mPageCount);
            int start=mPageCount-1;
            mPageCount=pageCount;
            notifyItemRangeChanged(start,diff);
        }
    }

    class PdfViewHolder extends RecyclerView.ViewHolder {
        PageView mPageView;
        public PdfViewHolder(@NonNull View itemView) {
            super(itemView);
            mPageView =itemView.findViewById(R.id.fragment_pdf_page_view);
        }

    }

    public void setBitmap(int position,Bitmap bitmap, float zoom, boolean wentBack, Link[] links, Quad[] hits) {
       PdfDetail detail=new PdfDetail(bitmap, zoom, wentBack, links, hits, position);
       notifyItemChanged(position,detail);
    }

    public void setError(int position) {
        notifyItemChanged(position,"error");
    }
    public static class PdfDetail{
        Bitmap bitmap;
        float zoom;
        boolean wentBack;
        Link[] links;
        Quad[] hits;
        int position;

        public PdfDetail(Bitmap bitmap, float zoom,
                         boolean wentBack, Link[] links, Quad[] hits, int position) {
            this.bitmap = bitmap;
            this.zoom = zoom;
            this.wentBack = wentBack;
            this.links = links;
            this.hits = hits;
            this.position = position;
        }
    }
}
