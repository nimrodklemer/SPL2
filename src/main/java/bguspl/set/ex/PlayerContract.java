package bguspl.set.ex;
interface PlayerContract{
    //@inv id >= 0
    //@inv score >= 0


    void terminate();
    void keyPressed(int slot);
    void point();
    void penalty();
    int getScore();
}