package bguspl.set.ex;
interface DealerContract{
    void terminate();
    boolean shouldFinish();
    void removeCardsFromTable();
    void placeCardsOnTable();
    void sleepUntilWokenOrTimeout();
    void updateCountdown();
    void resetCountdown();
    void removeAllCardsFromTable();
    void announceWinners();
}