# SPL2
Player.java line 99.
Maybe instead of dealer lock, make queue atomic.
consider using ConcurrentLinkedQueue for dealer sets queue with submitSets.

PRBLEMS THAT MAY OCCUR
1 notify on object in handleSet().
