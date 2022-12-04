package bguspl.set.ex;

import bguspl.set.Env;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer countdown times out (at which point he must collect the cards and reshuffle the deck).
     */
    private long countdownUntil;

    // submitted sets by players to dealer + first index is player's id.
    private int[] checkSet;

    private Thread dealerThread;

    public Object handleSetLock;


    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        this.handleSetLock = new Object();
        this.checkSet = null;
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        dealerThread = Thread.currentThread();
        System.out.printf("Info: Thread %s starting.%n", Thread.currentThread().getName());
        


        for (Player p : players) {
            Thread playerThread = new Thread(p, "Player " + p.id);
            System.out.println("FAIL 07");
            playerThread.start();
            System.out.println("FAIL 08");
        }

        while (!shouldFinish()) {
            Collections.shuffle(deck);

            for (Player p : players) {
                p.keyContinue = false;
            }

            placeCardsOnTable();

            for (Player p : players) {
                p.keyContinue = true;
            }
            countdownLoop();
            removeAllCardsFromTable();
        }
        for (Player p : players) {
            try{p.getThread().join();} catch(InterruptedException ex){}
        }
        announceWinners();
        System.out.printf("Info: Thread %s terminated.%n", Thread.currentThread().getName());
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void countdownLoop() {
        resetCountdown();
        while (!terminate && System.currentTimeMillis() < countdownUntil) {
            updateCountdown();
            sleepUntilWokenOrTimeout();
            if(checkSet != null){
                removeCardsFromTable();
                placeCardsOnTable();
            }
        }
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement

        // call players terminate
        for(Player p : players){
            p.terminate();
        }
        // set terminate to true

        terminate = true;

    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks if any cards should be removed from the table and returns them to the deck.
     */
    private void removeCardsFromTable() {
        // TODO implement

        // remove a set from the table.        
        
        // checking if legal and also rewarding or penalizing.

        if(handleSet(checkSet)){
            // remove cards and tokens from table
            for (int c = 1; c < checkSet.length; c++) {
                table.removeCard(checkSet[c]);
                for(Player p : players){
                    if(p.chosenSlots.contains(checkSet[c])){
                        p.chosenSlots.remove(checkSet[c]);
                    }
                }
            }   
            //resets time because set is found
            resetCountdown();
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        // TODO implement

        int card;
        // get list of the slots who miss a card
        List<Integer> slotsMissingCards = table.slotsMissingCards();

        while(deck.size() > 0 && slotsMissingCards.size() > 0){
            // takes card from the deck and place it in the slot
            card = deck.remove(0);
            table.placeCard(card, slotsMissingCards.remove(0));
        }
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement

        //wait for sleep countdown or for player to send a set.

        while(!dealerThread.isInterrupted() && System.currentTimeMillis() < countdownUntil){
            updateCountdown();
        }

    }

    /**
     * Update the countdown display.
     */
    private void updateCountdown() {
        // TODO implement
        Long secs = (countdownUntil-System.currentTimeMillis());
        env.ui.setCountdown(secs.intValue(), false);
    }

    /**
     * Reset the countdown timer and update the countdown display.
     */
    private void resetCountdown() {
        if (env.config.turnTimeoutMillis > 0) {
            countdownUntil = System.currentTimeMillis() + env.config.turnTimeoutMillis;
            updateCountdown();
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        // TODO implement

        for(int c = 0; c < env.config.tableSize; c++){
            deck.add(table.slotToCard[c]);
            table.removeCard(c);
        }
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {

        // TODO implement
        List<Integer> winners = new ArrayList<>();
        int maxScore = 0, tmpScore = 0;
        for(Player player : players ){
            tmpScore = player.getScore();
            if(maxScore < tmpScore){
                maxScore = tmpScore;
            }
       }
       for(Player player : players ){
            if(player.getScore() == maxScore){
                winners.add(player.id);
            }
        }
        env.ui.announceWinner(winners.stream().mapToInt(Integer::intValue).toArray());
    }

    /**
     * Check if a given set is legal and reward or penalize the player accordingly.
     * @return true if legal set.
     */
    private boolean handleSet(int[] setAndId){

        int playerId = setAndId[0];
        int[] set = new int[setAndId.length-1];


        for(int c = 1; c < set.length; c++){
            set[c-1] = setAndId[c];
        }
        //reset setAndId
        setAndId = null;

        boolean legal = env.util.testSet(set);
        
        if(legal){
            //reward player
            players[playerId].point();
        }
        else{
            //penalize player
            players[playerId].penalty();

        }
        System.out.println("FAIL 20");
        synchronized(players[playerId]){
            players[playerId].notify();
        }
        
        System.out.println("FAIL 21");

        return legal;
    }

    public void interruptDealer(int playerId){
        synchronized(players[playerId].waitBeforeInterruptPlayer){
            System.out.println("FAIL 05");
            dealerThread.interrupt();
            System.out.println("FAIL 06");
        }
        
    }

    public void submitSet(int id, int[] set){
        int len = set.length;

        int[] setAndId = new int[len+1];

        setAndId[0] = id;

        for(int c = 1; c < len; c++){
            setAndId[c] = set[c-1];
        }

        checkSet = setAndId;
    }
}
