package ir.smartdevelopers.pdf_view;

public interface ActionListener {
    void onPageViewSizeChanged(int w, int h);
    void gotoURI(String uri);
    void gotoPage(String uri);
    void toggleUI();
    void onPageViewZoomChanged(float zoom);
    void goBackward();
    void goForward();
}
