package bguspl.set.ex;

import bguspl.set.Env;

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
    private Queue<int[]> sets;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        System.out.printf("Info: Thread %s starting.%n", Thread.currentThread().getName());
        while (!shouldFinish()) {
            Collections.shuffle(deck);
            placeCardsOnTable();
            countdownLoop();
            removeAllCardsFromTable();
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
            removeCardsFromTable();
            placeCardsOnTable();
        }
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement

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
        int[] setAndId = sets.remove();
        
        
        while(!sets.isEmpty()){
            // checking if legal and also rewarding or penalizing.
            if(handleSet(setAndId)){
                // remove cards and tokens from table
                for (int c = 1; c < setAndId.length; c++) {
                    table.removeCard(setAndId[c]);
                }
            }
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
        try {
            Thread.sleep(env.config.turnTimeoutMillis);
        } catch (InterruptedException ignored) {}
    }

    /**
     * Update the countdown display.
     */
    private void updateCountdown() {
        // TODO implement
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

        boolean legal = env.util.testSet(set);
        
        if(legal){
            //reward player
            players[playerId].point();
        }
        else{
            //penalize player
            players[playerId].penalty();

        }

        return legal;
    }
}
