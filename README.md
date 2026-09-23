# Symbolic AI — Dots and Boxes Project

_L3 Computer Science — Université Paris-Saclay_

---

# Part 1: Game Analysis and Formalization

## Question 1 — Software Architecture Analysis

Analysis of the provided Javadoc and source code reveals an architecture organized around five central concepts: the game state, actions, the transition, the score, and replay handling.

### 1.1 Game State Representation

The game state is encapsulated in the `Board` class. It is composed of three main data structures:

|Component|Description|
|---|---|
|`boolean[][] hEdges`|`rows × (cols-1)` matrix of horizontal segments. `true` = segment drawn.|
|`boolean[][] vEdges`|`(rows-1) × cols` matrix of vertical segments. `true` = segment drawn.|
|`int[][] boxes`|`(rows-1) × (cols-1)` matrix of boxes. `-1` = free, `0` or `1` = owner.|

This representation corresponds directly to the set of states **S** of the theoretical formalism: a state _s ∈ S_ is entirely described by the contents of these three matrices together with the identifier of the active player, which is managed in `DotsBoxesGame`.

### 1.2 Action Modeling

Actions are represented by the immutable `Action` class, defined by two elements:

- **Type**: an enumeration, `HORIZONTAL` or `VERTICAL`, specifying the orientation of the segment.
- **`row` and `col`**: the coordinates of the segment in the board's internal representation.

Action validation is delegated to the `Board.isValid(Action)` method, which checks that the indices are within bounds and that the segment has not yet been drawn.

### 1.3 Transition Function

The transition function **T : S × A → S** is implemented by the method `Board.apply(Action action, int playerId)`:

```java
public int apply(Action action, int playerId) {
    // 1. Draw the segment (hEdges or vEdges)
    // 2. Check the adjacent boxes via isBoxClosed(r, c)
    // 3. Assign the closed boxes to player playerId
    // 4. Return the number of closed boxes (0, 1 or 2)
}
```

The private method `isBoxClosed(r, c)` checks whether the four sides of a box (top, bottom, left, right) are drawn and whether the box is still free (`boxes[r][c] == -1`).

### 1.4 Score Computation

The score is computed at two levels:

- **`Board.getScore(int playerId)`**: counts the number of boxes whose value in `boxes[][]` matches `playerId`. This is the raw score.
- **`Referee.getScore(Player player)`**: delegates the computation to internal counters updated after every move played. Penalties for invalid moves are also subtracted here.

The penalty for an invalid move is applied in `Referee.applyInvalidMovePenalty()`, which reduces the offending player's score by 1 point.

### 1.5 Replay Handling

The replay rule is handled in the main loop of `DotsBoxesGame.play()`:

```java
int gainedBoxes = referee.applyAction(currentPlayer, action);

// Essential rule: the player plays again if they close one or more boxes
if (gainedBoxes == 0) {
    switchPlayer(); // Only switch player if no box was closed
}
```

If `gainedBoxes > 0`, the current player keeps the turn and plays again immediately. This mechanism is what makes the game **not strictly alternating**.

---

## Question 2 — Formal Definition of the Dots and Boxes Game

A Dots and Boxes game is defined by a quintuple _⟨P, S, A, T, U⟩_.

### 2.1 The Players P

The set of players is _P = {p0, p1}_, where _p0_ is the player with identifier 0 (blue) and _p1_ the player with identifier 1 (red). Each player implements the `Player` interface, which exposes the methods `getAction(Board)` and `getId()`. The concrete implementations are `HumanPlayer`, `AutomatePlayer` and `AIPlayer`.

### 2.2 The Set of States S

A state _s ∈ S_ is a quadruple describing the complete configuration of the board:

- _H ∈ { 0, 1 }^{rows × (cols-1)}_: matrix of horizontal segments (`hEdges`)
- _V ∈ { 0, 1 }^{(rows-1) × cols}_: matrix of vertical segments (`vEdges`)
- _B ∈ { -1, 0, 1 }^{(rows-1) × (cols-1)}_: matrix of box owners (`boxes`)
- _j ∈ { 0, 1 }_: identifier of the active player (`currentPlayer` in `DotsBoxesGame`)

The initial state _s0_ corresponds to a completely empty board: _H = 0_, _V = 0_, _B = -1_ everywhere, and _j = 0_. A state is **terminal** when `board.isFinished()` returns `true`.

### 2.3 The Set of Actions A

- _A_H = { (HORIZONTAL, r, c) | 0 ≤ r < rows, 0 ≤ c < cols-1, H[r][c] = 0 }_
- _A_V = { (VERTICAL, r, c) | 0 ≤ r < rows-1, 0 ≤ c < cols, V[r][c] = 0 }_

The set of actions available from a state _s_ is returned by `Board.getAvailableActions()`.

### 2.4 The Transition Function T

_T(s, a) = s'_ where:

1. The segment corresponding to _a_ is drawn in _H_ or _V_
2. Every adjacent box whose four sides are drawn is assigned to the active player in _B_
3. If at least one box is closed, _j_ is unchanged (replay)
4. Otherwise, _j_ is switched to the other player: _j' = 1 - j_

This function is implemented jointly by `Board.apply()` (matrix updates) and `DotsBoxesGame.play()` (active player management). Deep copying of the board is provided by the `Board(Board other)` constructor.

### 2.5 The Utility Function U

_u_j(s) = score_j(s) - penalties_j(s)_

where _score_j(s)_ is the number of boxes captured by player _j_ in the terminal state _s_, and _penalties_j(s)_ is the number of invalid moves played by _j_ (each worth -1 point).

Since the game is not strictly zero-sum (penalties reduce each player's score independently), it is technically a **variable-sum** game, although it behaves like a zero-sum game in the absence of penalties.

---

## Question 3 — Game Tree Analysis

### 3.1 Initial Branching Factor

For a grid of _N × M_ dots, the total number of playable segments is:

- Horizontal segments: _N × (M-1)_
- Vertical segments: _(N-1) × M_

|Grid|Initial branching factor b₀|
|---|---|
|3 × 3 (2×2 boxes)|12 segments|
|4 × 4 (3×3 boxes)|24 segments|
|5 × 5 (4×4 boxes)|40 segments|

### 3.2 Evolution of the Branching Factor

With every move played, one segment is permanently drawn and removed from the list of available actions. The branching factor therefore **decreases linearly** over the course of the game, which is directly visible in the code: `Board.getAvailableActions()` returns a list whose size decreases by 1 with each call to `Board.apply()`.

### 3.3 Maximum Depth of the Game Tree

The maximum depth equals the initial branching factor, since each level of the tree corresponds to drawing one segment. The complexity of the full game tree is therefore **O(b₀!)**, which makes exhaustive exploration impossible from a 4×4 grid onwards (24! ≈ 6.2 × 10²³ nodes). This is why Minimax is necessarily used with a **limited depth**.

### 3.4 Is the Game Strictly Alternating?

No. The replay rule makes the game **not strictly alternating**. A player can chain several consecutive moves if they close one or more boxes on each turn.

> **Consequence for Minimax:** The classic Minimax algorithm assumes strict alternation between a MAX player and a MIN player. In `MinimaxActionStrategy.minimax()`, the current player `currentPlayerId` is only switched if `nbFerme == 0`. If a box is closed, the same player continues at the next level:
>
> ```java
> if (nbFerme > 0) {
>     res = minimax(boardCopy, playerId, depth-1, currentPlayerId); // replay
> } else {
>     res = minimax(boardCopy, playerId, depth-1, 1-currentPlayerId); // alternation
> }
> ```

---

## Question 4 — Strategic Analysis

### 4.1 Closing a Box

The simplest situation is a direct closure: a box that already has three of its four sides drawn. Playing the fourth side immediately earns a point and allows the player to play again. The method `Board.isActionClosing(Action)` detects exactly this situation and is used by the greedy strategy `GloutonActionStrategy`.

### 4.2 Chains of Boxes

A **chain of boxes** is a sequence of adjacent boxes, each having exactly two or three sides drawn, forming a path. When a player closes the first box of a chain, they are forced to close all the following ones, but the **last box of the chain** often forces them to hand a new box to the opponent.

This is precisely the phenomenon illustrated in Figure 1 of the assignment: the red player exploits a chain of boxes at the end of the game and wins 6-3 despite an apparent disadvantage. The greedy strategy, which systematically closes any available box, falls into this trap.

### 4.3 Dangerous Positions: Three-Sided Boxes

A **three-sided** box is a dangerous position: the player who draws the third side offers that box to the opponent, who will close it on the next turn and may chain further moves. A good strategy must avoid creating such positions, anticipate the opponent's chains, and know when to sacrifice a box in order to control the rhythm of the game.

### 4.4 Limits of Naive Strategies

|Strategy|Behavior and limits|
|---|---|
|**Random**|Plays a random move. Never intentionally closes a box and may create unfavorable three-sided positions. Average score ≈ 50% of the boxes.|
|**First valid**|Always plays the first available segment in list order. Predictable and highly suboptimal.|
|**Greedy**|Closes a box whenever possible, otherwise plays randomly. Effective in the short term but exploitable through chains of boxes.|
|**Minimax**|Anticipates the opponent's moves over a limited depth. The only one able to handle chains of boxes, but computationally expensive.|

These observations fully justify the introduction of Minimax in Part 3: naive strategies are unable to anticipate chains of boxes and are systematically exploited by an opponent who plans several moves ahead.

---

# Part 2: Automata and Experimental Comparison

## Question 1 — Random Automaton

### 1. Principle

The random automaton is the simplest conceivable strategy. On each turn, it retrieves the list of all available actions on the board via `board.getAvailableActions()`, then selects one uniformly at random.

### 2. Implementation

The `RandomActionStrategy` class implements the `ActionStrategy` interface (Strategy pattern):

```java
public Action selectAction(Board board, int playerId) {
    List<Action> actionsAvailable = board.getAvailableActions();
    if (actionsAvailable.isEmpty()) {
        return null;
    }
    return actionsAvailable.get(random.nextInt(actionsAvailable.size()));
}
```

**Key points:**

- The method returns `null` if no action is available (full board), in accordance with the contract of the `ActionStrategy` interface.
- A single `Random` object is used for the random draw.
- The implementation never modifies the board: it merely reads the available actions.

### 3. Validation Through Tests

The `RandomActionStrategyTest` class validates the following behaviors:

- The returned action is always valid (it belongs to the list of available actions).
- `null` is returned if the board is finished (no actions left).
- The automaton respects the `ActionStrategy` interface and can be used as the strategy of an `AutomatePlayer`.

---

## Question 2 — Greedy Automaton

### 1. Principle

The greedy automaton improves on the random strategy by exploiting the fundamental rule of the game: closing a box earns a point **and** lets the player play again immediately. The idea is therefore to always prioritize a move that closes a box whenever one is possible. If no closing move is available, the greedy automaton falls back on a random move.

### 2. Implementation

The `GloutonActionStrategy` class implements the `ActionStrategy` interface:

```java
public Action selectAction(Board board, int playerId) {
    List<Action> actionsAvailable = board.getAvailableActions();
    // 1. Look for an action that closes a box
    for (Action action : actionsAvailable) {
        if (board.isActionClosing(action)) {
            return action;
        }
    }
    // 2. Otherwise, play a random move
    if (actionsAvailable.isEmpty()) {
        return null;
    }
    return actionsAvailable.get(random.nextInt(actionsAvailable.size()));
}
```

**Key points:**

- The method `board.isActionClosing(Action)` checks whether drawing a segment completes the four sides of at least one adjacent box.
- If several closures are possible, the greedy automaton picks the first one found in the list's iteration order.
- The constructor accepts an optional `seed` parameter to make the behavior reproducible during tests.

### 3. Validation Through Tests

The `GloutonActionStrategyTest` class validates the following behaviors:

- When a box can be closed (3 sides drawn), the greedy automaton systematically plays the fourth side.
- When no closure is possible, the returned action is valid and belongs to the available actions.
- The greedy automaton does not modify the board during selection (it only reads).

### 4. Analysis of the Limits

The limits of the greedy automaton are analyzed in detail in the experimental comparison (Question 3). Its main weakness is its inability to anticipate: by playing randomly when no closure is available, it unintentionally creates three-sided boxes that the opponent immediately exploits. Moreover, when it closes the first box of a chain, it triggers a cascade of closures from which the opponent benefits.

---

## Question 3 — Experimental Comparison: Random vs Greedy

### 1. Experimental Protocol

To compare the two strategies objectively, we set up an automated benchmark of **100 games** on grids of random size between 3×3 and 13×13, using the standard `DotsBoxesGame` engine with the full rules (replay, penalties).

### 2. Results

|Result|Count|Percentage|
|---|---|---|
|Random wins|7 / 100|7 %|
|Greedy wins|90 / 100|90 %|
|Draw|3 / 100|3 %|

The greedy strategy dominates by a wide margin, with **90 wins out of 100 games**.

### 3. Analysis

**Why does Greedy dominate?** The greedy strategy exploits the fundamental rule of the game: closing a box earns a point **and** allows an immediate replay. Each closed box grants an extra turn, creating a **snowball** effect, while the random player wastes its turns on segments with no immediate value.

**Limits of Greedy — chains of boxes:** When a chain of several boxes is available, the greedy strategy closes the first box and is then forced to hand the following ones to the opponent. An expert player would intentionally sacrifice 2 boxes to force the opponent to open a shorter chain (the _double-dealing strategy_).

**Limits of Greedy — three-sided boxes:** By playing randomly when no closure is available, the greedy strategy unintentionally creates **three-sided boxes** that the opponent can close immediately. It never anticipates the consequences of its moves on the future state of the board.

**Limits of Random:** The random strategy ignores every closing opportunity and constantly creates dangerous positions. Its 7% of wins can be explained solely by luck on small grids.

### 4. Conclusion

These results show that both strategies are fundamentally **myopic**: they only optimize immediate gain without anticipating future consequences. This justifies the introduction of **Minimax** in Part 3, which will be able to anticipate chains of boxes, avoid dangerous positions, and adopt an optimal strategy several moves ahead.

---

# Part 3: Minimax and Alpha-Beta

## Question 1 — Implementing Minimax with Limited Depth

### 1. Principle of the Algorithm

Minimax is an adversarial search algorithm that explores the game tree by alternating between a MAX player (who seeks to maximize their score) and a MIN player (who seeks to minimize it). In our implementation, the search is bounded by a `maxDepth` parameter that defines the number of levels explored before the leaves are evaluated.

### 2. Implementation Architecture

The `MinimaxActionStrategy` class implements the `ActionStrategy` interface and relies on two main methods:

**`selectAction(Board board, int playerId)`** — Entry point. For each available action, it applies the move on the board, recursively evaluates the subtree via `minimax()`, then undoes the move (`board.undo()`). The best move is retained. If several moves share the best value, a random draw breaks the tie.

**`minimax(Board board, int depth, int currentPlayerId, int playerId, int score)`** — Recursive core of the algorithm. The `score` parameter is propagated incrementally: each time a box is closed, the relative score is adjusted by `+nbFerme` or `-nbFerme` depending on whether it is our player or the opponent who closes it.

### 3. Handling the Non-Strictly-Alternating Game

Dots and Boxes allows a player to play again after closing a box. This rule is handled directly in the recursion:

```java
int nextPlayer = (nbFerme > 0) ? currentPlayerId : 1 - currentPlayerId;
```

When `nbFerme > 0`, the current player keeps the turn. The MAX ↔ MIN change of perspective only occurs if no box is closed (`nbFerme == 0`).

### 4. Depth Configuration

The maximum depth is defined at construction: `new MinimaxActionStrategy(maxDepth)`. On each recursive call, `depth` is decremented by 1. When `depth == 0`, the search stops and returns the current score (static evaluation).

**Note on replay and depth:** in this base version, the depth is always decremented, even on a replay. This choice was made to strictly control computation time. The dynamic extension of depth on replay will be implemented in the Alpha-Beta version (Part 4).

### 5. Node Counter

A `NodeCounterObserver` is integrated to measure the number of nodes visited during each search. It is incremented on every recursive call (`observer.increment()`) and reset on every call to `selectAction()` via `observer.reset()`.

### 6. TDD Validation

The `MinimaxActionStrategyTest` class contains 15 unit tests organized into four categories:

**1. Terminal states (3 tests):** verification on a full board, return of the relative score, and static evaluation at depth 0.

**2. Optimal decisions on simple configurations (3 tests):** direct closure on a 2×2 board, preference for winning a box, avoidance of offering a box.

**3. Consistency of returned values (4 tests):** value bounded between `-nbCases` and `+nbCases`, player symmetry, non-null return when moves are available, valid action.

**4. Node counter (5 tests):** initialization at zero, positive counter after selection, reset, growth with depth, growth with board size.

All tests pass successfully.

---

## Question 2 — Impact of Each Heuristic Function Criterion

### 1. Current Score Difference

```java
score = board.getScore(playerId) - board.getScore(1 - playerId)
```

This is the basis of the evaluation. It directly reflects the advantage in captured boxes. However, on its own it is **insufficient**, because it only sees what has already happened, not what is going to happen.

### 2. Three-Sided Boxes (`score -= chain * 3`)

This is the most important criterion. A three-sided box means the opponent can capture it **immediately** on the next move and **play again**. The penalty of 3 is deliberately larger than the value of a box (1) so that Minimax **actively avoids** creating these situations.

### 3. Chain Detection (`chainLength * 3`)

This is the most significant improvement. A chain of _n_ adjacent three-sided boxes means the opponent can capture **n boxes in one go** thanks to the replay rule. The penalty is proportional to the length of the chain.

### 4. Two-Sided Boxes (`score -= 1`)

This is a **preventive** criterion. A two-sided box is not yet dangerous but it **may** become a three-sided box. The small penalty of 1 lets Minimax take it into account without over-weighting this criterion.

### Interaction Between the Criteria

At low depth, the heuristic compensates: three-sided boxes and chains are crucial. At high depth, Minimax already sees the dangers and the heuristic refines borderline decisions. The lower the depth, the more important criteria 2, 3 and 4 become.

---

## Question 3 — Experimental Analysis of Search Depth

### 1. Experimental Protocol

To analyze the impact of search depth, we developed two benchmark classes: `MinimaxBenchmark` and `AlphaBetaBenchmark`. Each benchmark simulates complete games (the AI plays both sides by itself) and measures the average time per decision (in milliseconds), the total number of nodes explored per game, and, for Alpha-Beta, the number of alpha cutoffs and beta cutoffs.

Tests are run on three grid sizes (3×3, 4×4, 5×5) and five depths (1 to 5), with 3 runs per configuration. A safeguard aborts the search if a configuration exceeds 5 seconds per decision.

### 2. Results for Minimax

|Grid|Depth|Average time/decision|Nodes/game|
|---|---|---|---|
|3×3|1|<1 ms|~120|
|3×3|2|<1 ms|~1,400|
|3×3|3|~2 ms|~15,000|
|3×3|4|~30 ms|~150,000|
|3×3|5|~400 ms|~1,300,000|
|4×4|1|<1 ms|~480|
|4×4|2|~5 ms|~11,000|
|4×4|3|~150 ms|~250,000|
|4×4|4|too slow|—|
|5×5|1|<1 ms|~1,600|
|5×5|2|~40 ms|~60,000|
|5×5|3|too slow|—|

_Note: exact values vary depending on the machine; the order of magnitude is representative._

### 3. Impact of the Branching Factor

The combinatorial explosion is clearly visible in the results. The number of nodes grows approximately as O(b^d). Considering a constraint of **1 second per decision**, the reasonable depths are:

|Grid|Maximum reasonable depth (Minimax)|
|---|---|
|3×3|5|
|4×4|3|
|5×5|2|

### 4. Quality of Play by Depth

Results of Minimax against the greedy strategy over 50 games (3×3 grid):

|Depth|Wins vs Greedy|Observations|
|---|---|---|
|1|~55%|Nearly random, no significant advantage|
|2|~75%|Starts to avoid immediate traps|
|3|~90%|Handles short chains|
|4-5|~98%|Effectively anticipates long chains|

The quality of play improves drastically between depths 2 and 3: it is from depth 3 onwards that Minimax starts to "see" chains of boxes and to avoid creating three-sided boxes.

---

## Question 4 — Implementing Alpha-Beta and Comparing with Minimax

### 1. Alpha-Beta Implementation

The `AlphaBetaActionStrategy` class reuses the structure of Minimax and adds Alpha-Beta pruning. Two additional parameters are propagated through the recursion:

- **alpha**: the best value guaranteed for the MAX player (lower bound).
- **beta**: the best value guaranteed for the MIN player (upper bound).

When `alpha ≥ beta`, the current subtree can no longer influence the final decision. The code distinguishes alpha cutoffs (MIN node) from beta cutoffs (MAX node) via dedicated counters in `AlphaBetaPruningObserver`.

```java
if (alpha >= beta) {
    observer.incrementAlphaCut(); // or incrementBetaCut()
    break;
}
```

**Improvements over Minimax:**

- **Advanced replay handling:** Alpha-Beta does not decrement the depth when a box is closed (`nextDepth = (nbFerme > 0) ? depth : depth - 1`), which allows an entire chain to be captured as a single logical move.
- **Iterative Deepening:** `selectAction()` starts the search at depth 1, then increments the depth until time runs out (`TimeOutException`). This guarantees that a move is always available.
- **Time management:** a `deadline` is computed on entry to `selectAction()` and checked at every node via `checkTime()`.
- **Action sorting:** `board.getSortedActions()` orders the actions to favor early cutoffs.

### 2. TDD Validation

The `AlphaBetaActionStrategyTest` class contains 14 unit tests covering: terminal states, optimal decisions, value consistency, counters and cutoffs.

### 3. Experimental Comparison: Minimax vs Alpha-Beta

#### Number of Nodes Explored

|Algorithm|Nodes explored (3×3, depth 5)|Reduction|
|---|---|---|
|Minimax|~1,300,000|—|
|Alpha-Beta|~80,000|**×16**|

#### Computation Time

|Grid|Depth|Minimax (ms/decision)|Alpha-Beta (ms/decision)|Speedup|
|---|---|---|---|---|
|3×3|3|~2 ms|<1 ms|×3|
|3×3|5|~400 ms|~25 ms|×16|
|4×4|3|~150 ms|~8 ms|×19|
|4×4|5|impractical|~200 ms|—|
|5×5|3|impractical|~60 ms|—|

#### Reachable Depth (in 1 Second)

|Grid|Minimax|Alpha-Beta|Gain|
|---|---|---|---|
|3×3|5|8+|+3|
|4×4|3|5-6|+2-3|
|5×5|2|4|+2|

#### Quality of Decisions

At equal depth, Alpha-Beta returns exactly the same decisions as Minimax. Pruning only removes branches that cannot influence the final result. The real gain in quality comes from the greater depth reachable within the same time budget.

#### Conditions for Maximal Pruning

Alpha-Beta pruning is most effective when the **best moves are evaluated first**. Our implementation exploits this through action sorting (`board.getSortedActions()`) and Iterative Deepening. On a 4×4 grid at depth 5, we typically observe:

- **Alpha cutoffs**: ~3,000–5,000
- **Beta cutoffs**: ~3,000–5,000

These cutoffs explain the ×16 to ×20 reduction in the number of nodes explored compared to Minimax.

---

# Part 4: Towards an Expert AI Player

## 4.1 Alpha-Beta Algorithm with a Transposition Table (TT)

### 4.1.1 Inspiration and Initial Idea

The Dots and Boxes search tree generates a great many redundant paths. For example, drawing segment A and then segment B leads to exactly the same grid state as drawing B and then A (provided that no box closure occurs). In game algorithmics, such paths are called **transpositions**. With a classic Alpha-Beta algorithm, the engine needlessly recomputes the full evaluation of the two identical subtrees.

To eliminate this massive redundant computation, we implemented a system that memorizes already visited states: a **Transposition Table (TT)**.

### 4.1.2 Technical Choices: Zobrist Hashing and Transposition Table

#### A. Why Zobrist Hashing?

We implemented Zobrist hashing, the standard for board game AIs, for three reasons:

- **History independence:** Thanks to the binary XOR operator (`^`), the hash is commutative. Whether the segments are drawn in the order A-B or B-A, the final result is identical.
- **Algorithmic efficiency:** Updating the hash is an extremely fast O(1) operation.
- **Collision minimization:** Using 64-bit numbers (`long`) provides a space of 2^64 possible values.

#### B. Data Structure and Management (TTEntry)

Each entry stored in the table (`TTEntry`) contains:

- **The depth (depth):** A memorized value is only reused if it was computed at an equal or greater depth.
- **The validity flags (Flags):** `EXACT` (precise value), `LOWER_BOUND` (resulting from a Beta cutoff), `UPPER_BOUND` (resulting from an Alpha cutoff).
- **The best move (bestAction):** By storing the best move found during a previous search, we can try it first the next time this state is encountered (Move Ordering), which drastically increases the probability of triggering early cutoffs.

#### C. Managing the Storage Structure: TranspositionTable

The `TranspositionTable` class acts as a short-term memory. Its access is instantaneous (O(1) complexity via `HashMap`), but its management poses a challenge: combinatorial explosion.

To avoid memory saturation, we implemented a strict capacity limit (`TAILLE_MAX`). When the table is full, it is emptied entirely. Paradoxically, this dynamic cleanup allowed us to increase the hit rate throughout the game, because the table immediately fills with fresh states related to the branch currently being explored. The `clear()` method is also called at the start of every new match.

### 4.1.3 Advanced Search Tree Optimizations

**A. Move Ordering:** The `reordonnerAvecTT` method queries the Transposition Table and places the already recorded `bestAction` in first position, triggering very early pruning.

**B. Dynamic Horizon Extension (Replay Handling):** If an action closes a box, the search depth is not decremented. This allows our AI to evaluate an entire chain of boxes as a single move, reaching apparent depths of more than 20.

**C. Integration of an Expert Heuristic:** At leaf nodes, the algorithm invokes `heuristicv2`, an evaluation function capable of analyzing chains of boxes, anticipating parity theory and evaluating mobility without instantiating a single object in memory.

---

## 4.2 Testing and Performance Evaluation: The AIArena Infrastructure

To go beyond manual testing, we developed the `AIArena` class. This tool simulates automated matches while faithfully reproducing the constraints of the final tournament, with home-and-away matches (roles are swapped to compensate for the first/second player advantage) and strict adherence to the clock.

`AIArena` also served as a diagnostic tool, allowing us to analyze the exploration depth and the computation time per node. This analysis led us to implement a **dynamic time allocation** based on the number of remaining segments:

_allocated_time = remaining_time / (remaining_segments / K)_

By tuning the constant K, the AI "takes its time" in complex openings and then speeds up in the endgame, when the transposition table lets it solve the board almost instantly.

---

## 4.3 Heuristic Improvement

### 4.3.1 Architecture and Tactical Concepts of Heuristic v2

Integrating the Transposition Table exposed the poor performance of our original evaluation function. We designed `heuristicv2`, whose philosophy rests on **allocation-free performance** and **structural analysis of the board**.

**1. Low-level matrix evaluation (the cost of mobility):** The heuristic scans the boolean matrices of horizontal and vertical edges directly, with an asymmetric penalty: closing 1 or 2 boxes yields a massive positive bonus, while giving away a box incurs a heavy penalty.

**2. Chain analysis via Union-Find (endgame anticipation):** To detect chains without slowing down the computation, we implemented a Union-Find structure. It groups adjacent boxes (≥ 2 sides drawn) into connected components using simple integer arrays (`parent`, `rank`, `compSize`), and distinguishes chains (with a 3-sided entry) from loops (closed cycles of 2-sided boxes).

**3. Advanced strategy: Parity and Double-Dealing:** By computing the parity of the remaining safe moves and the number of long chains, the heuristic determines which player will be forced to open the first chain (forced opening). It also automatically simulates the expert _double-dealing_ strategy: the algorithm computes a net gain that assumes the AI will deliberately sacrifice the last 2 boxes of a chain to force the opponent to open the next chain.

**4. Dynamic weights according to game phase:** The heuristic computes a ratio representing the game phase (edges played / total). Mobility is strongly valued early in the game, while chain control and danger penalties become the dominant criteria in the endgame.

---

## 4.4 Alternative Approach: Monte-Carlo Tree Search (MCTS)

### 4.4.1 Principle of the Algorithm

MCTS relies on four phases repeated in a loop:

1. **Selection**: Starting from the root, descend the tree by choosing at each node the child that maximizes the UCT formula: `UCT = (wins / visits) + C × √(ln(parent_visits) / visits)` where `C = 1.0`.
2. **Expansion**: When a node still has unexplored actions, select one and create a child node.
3. **Simulation (rollout)**: Simulate a semi-informed random game until the end.
4. **Backpropagation**: Propagate the result (win/loss/draw) back along the path to the root.

### 4.4.2 Implemented Optimizations

**Multi-thread parallelization:** A thread pool sized to the number of available cores. Each thread builds its own MCTS tree independently and the results are merged by aggregation (_root parallelization_).

**Semi-informed simulations:** Three priority levels: (1) close a box (`isActionClosing`), (2) play a random safe move, (3) a completely random move as a last resort.

**Early victory detection:** If a player owns more than half of the total boxes during a rollout, the simulation stops immediately.

**Heuristic bias (Prior Knowledge):** During expansion, the heuristic score is converted into a probability via a sigmoid and injected as 10 "phantom visits" that bias the UCT exploration.

**Swap-and-pop technique:** O(1) removal of an action from the list during rollouts.

### 4.4.3 Observed Limits

Despite these optimizations, the MCTS agent remains **inferior to Alpha-Beta + TT** on all grid sizes. This is explained by the highly tactical nature of Dots and Boxes: chains of boxes create deterministic cascade effects that Alpha-Beta captures exactly, whereas MCTS evaluates them probabilistically with high variance.

---

## 4.5 Pondering: Computing During the Opponent's Turn

To maximize effective computation time, we implemented a **pondering** mechanism in `ExpertActionStrategy`:

1. **After returning its move**, the Expert agent clones the board, applies its own move, and launches a daemon thread (`ponderThread`) that explores the tree from the resulting state.
2. **While the opponent is thinking**, the thread runs an Iterative Deepening Alpha-Beta, feeding the Transposition Table.
3. **When it is the agent's turn again**, the thread is interrupted and the agent has a pre-filled transposition table at its disposal.

```java
Board clonedBoard = new Board(board);
int nbFerme = clonedBoard.apply(meilleurCoupTrouve, playerId);
int nextPlayerId = (nbFerme > 0) ? playerId : 1 - playerId;

ponderThread = new Thread(() -> {
    while (depth <= actionsSize && !Thread.currentThread().isInterrupted()) {
        alphaBetaTT(clonedBoard, depth, nextPlayerId, ...);
        depth++;
    }
});
ponderThread.setDaemon(true);
ponderThread.start();
```

**Observed impact:** Particularly effective when the opponent plays the anticipated move — the initial decision time is often reduced to <50 ms instead of >500 ms.

---

## 4.6 Internal Tournament Results

Full tournament via `AIArena` on 3×3, 4×4, 4×5, 5×5, 6×6 and 6×8 grids (home-and-away matches). Win rate of agent A (rows) against agent B (columns):

|Agent A ↓ \ Agent B →|Random|Greedy|Alpha-Beta (no TT)|Alpha-Beta + TT|Expert (TT + Pondering)|MCTS|
|---|---|---|---|---|---|---|
|**Greedy**|~90%|—|—|—|—|—|
|**Alpha-Beta (no TT)**|~100%|~95%|—|—|—|—|
|**Alpha-Beta + TT**|~100%|~100%|~80%|—|—|—|
|**Expert**|~100%|~100%|~85%|~55%|—|~90%|
|**MCTS**|~100%|~95%|~50%|~20%|~10%|—|

**Measured gain compared to Alpha-Beta alone:**

- On 3×3 and 4×4 grids: marginal gain (~55-60% wins).
- On 5×5 and 6×6 grids: significant gain (~80% wins).
- On the 6×8 grid: very clear advantage (~90% wins).

These results demonstrate a **measurable and significant gain** of the Expert agent over Minimax and Alpha-Beta alone, mainly thanks to the Transposition Table, heuristic v2, and Pondering.
