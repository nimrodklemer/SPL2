package bguspl.set.ex;
interface TableContract{




    int countCards();
    void placeCard(int card, int slot);
    void removeCard(int slot);
    void placeToken(int player, int slot);
    boolean removeToken(int player, int slot);

}