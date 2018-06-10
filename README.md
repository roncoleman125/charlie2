##Charlie
Everyone probably knows about the winning hand, Blackjack.
Then, there's the less famous, less probable but more profitable,
*Charlie* which is a hand of five cards that does not break.

Charlie is an extensible, actor-based Blackjack system for teaching purposes
developed mostly by me.
It is a client-server, multiplayer system
that runs on multiple hosts and can exploit
[hyper-threading](http://en.wikipedia.org/wiki/Hyper-threading).

Charlie is built on plug-in modules that install at run-time
rather compile time.
The big advantage of plugins is that Charlie can be extended without modifying
its core functionality. The plugins are defined by Java interfaces.

Finally, Charlie uses [actors](http://en.wikipedia.org/wiki/Actor_model)
as its model of distributed computing.
Actors are a very elegant in concept which has been making a
comeback in recent years as an alternative to other communication /
synchronization methods.
Actors are, rightly, at the bottom of the Charlie software stack, below the
UI and plugins. So plugin developers may never encounter the actors or need to know
much about them.

###Why Blackjack?
Games are powerful tools in their own right for teaching.
Blackjack, in particular, is an example of an excellent game design, that is,
it is easy to play yet difficult to master.

Yet Blackjack has the potential to teach many things, including
but not limited to game theory, probability and statistical theory,
and in the case of Charlie, concurrency, threads, synchronization,
Artificial Intelligence, real-time computing, animation, etc.
While I hope Charlie can teach all these things, the plain truth is
Blackjack is also fun and may prove useful if you're ever stuck in Vegas, or
wherever your travels may take you.

This document does not teach Blackjack. There's tons of information in
books and on the Internet for that which I will reference.
My primary focus is to explore how to extend Charlie through its
plugin system.

###A brief history of Blackjack
The roots of Blackjack roots go back to Don Quixote and Seville and the 17th
century.
However, serious study of Blackjack began in the 1950s with long-running computer
simulations at IBM to discover what would become known as the [Basic Strategy](http://en.wikipedia.org/wiki/Blackjack), 
which gives the rules of "correct" play.
Issues of how to bet are not covered by the Basic Strategy but
[card counting](http://en.wikipedia.org/wiki/Card_counting).
[Kelly's criterion](http://en.wikipedia.org/wiki/Kelly_criterion),
was formulated in the 1950s for investing and can also be applied, in addition
to card counting, as an optimal betting strategy.
By the 1960s, MIT professor, E.O. Thorpe, published
[Beat the Dealer](http://goo.gl/BDQ83E), which for a while caused casinos to
change the Blackjack game design
to counteract the Basic Strategy.
However, these tactics backfired as the countermeasures
slowed the game which turned away customers.
It forced casinos to go back to the simpler "21" rules and look for other means to thwart
player the Basic Strategy and card counting.

Peter Griffin published [The Theory of Blackjack](http://goo.gl/kHQWjy) in the late 1990s.
It laid
out what some might regard as the definitive mathematical treatment of Blackjack from
the player's perspective.
In 2003, Ben Mezrich published [Bringing Down the House](http://goo.gl/HJK5KN),
which detailed the Las Vegas exploits of students
and their MIT professor. The story was dramatized by the popular
2008 film, [21](http://goo.gl/oEB8sv).

In my own case, I have studied Blackjack in-depth, asking whether a machine could learn to play
and bet. The published papers are [here](http://foxweb.marist.edu/users/ron.coleman/).
In 2010, I developed a 
[system](https://code.google.com/p/scaly/) related to Charlie
except written in [Scala](www.scala-lang.org) rather
than Java.
Unlike Charlie, the Scala version was not
distributed and had no plugins, GUI, animations, or sounds. It was really
a bare bones application for demonstration purposes.

###Basic ideas
Charlie is uses a client-server architecture organized around the
[model view controller](http://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93controller) design pattern. In
this pattern the dealer behaves like a controller, the client side like a view
and the house like the model.
After a "real" player logs in and establishes a connection, an instance of *House* constructs
an instance of *Dealer* for the player. The player is bound to this *Dealer* until the player
logs out.

Charlie is multi-player. However, players do not typically  play one another. Instead,
depending on the plugin configuration,
Dealer may allocate up to two bots that simulate real players.
If no bots have been configured, the game is "heads up," that is, the player
against Dealer. Yet Charlie supports multiple dealers concurrently which is
the multi-player basis.

Having said that, there is no reason of principle that the
bots could not also be "real" players. This is because Dealer
interacts with instances of *IPlayer*, the implementation of which
may be a "real" player or a bot player.

Dealer keeps a copy of player hands and implements the play rules, e.g., determining
the sequence of players, executing play
requests, deciding wins, losses, etc.
The Dealer broadcasts the game state to all players. For instance, when the Dealer
deals a card, Dealer sends the card all players.
Thus, all players can "see" the table.
The player's job is to process render the cards and wait for its turn.
For instance, if a player receives Ace+10, this
is of course a Blackjack.
However, the player doesn't have to determine fact. Dealer, after send this
hand to all players,
broadcasts "blackjack" for everyone.

A key design feature is hands are not passed around among players
or over the network.
Instead, Charlie uses _hand ids_.
A hand id is a unique key for a hand. Thus, when Dealer transmits a card,
Thus, when Dealer hits a hand, it sends a *Card* object and a hand id.
If the corresponding hand does not belong to a given player, the player
can ignore the card. If however the hand id corresponds to a hand
a player owns, the player has to respond. The permissible responses are
hit, stay, double-down, and in theory, surrender and split which are not
implemented.

If, of course, the player busts on a hit or double-down, there is
no permissible request for the player to make of Dealer. The game is over for
that player. In that case, Dealer tells the player it has broke so the player
just needs to wait for the end game signal from the Deal before starting
again.

Dealer only communicates with instances of *IPlayer*, a Java interface.
Dealer mostly doesn't know or care if IPlayer is a real player or a bot.
Dealer starts a new game only when 
Dealer receives a bet from *RealPlayer* which is an implementation of IPlayer. Although RealPlayer
interfaces to a "real" player on the client side, Deal is completely unaware
of the networking hierarchy. As the controller in MVC, Dealer is the controller
with different views.

###Plugins
Charlie has six (6) types of plugins:

1. Shoe
2. B9 bot
3. N6 bot
4. Gerty bot
5. Side bet
6. Advisor

The properties file, _charlie.props_, declares the plugin
values. Charlie checks charlie.props and if the plugin is
not found or there is an exception trying to instantiate the plugin,
Charlie recovers gracefully and runs without the plugin. The only
exception the shoe plugins. Charlie requires a shoe and
the server crashes without one. 

###Shoe plugin
A shoe contains instances of *Card* objects from which Dealer deals to players.
A shoe must implement *IShoe*, a Java interface.
Built into Charlie is a concrete class, *Shoe*, which implements IShoe.
This class has six decks for "serious" play and/or training.
In general, however, an IShoe may contain as many or as few cards
as necessary.
Thus, shoes are very useful for debugging purposes.

There is a property, _charlie.shoe_ in the properties files, charlie.props. The value
must be a fully qualified
instance of IShoe. Here's an example

    charlie.shoe charlie.card.Shoe01

The key is "charlie.shoe" and the value is "charlie.card.Shoe01".
It turns out the Shoe01 is a one-deck shoe which I've found helpful for testing.

When House constructs Dealer, Dealer looks for the shoe property and tries to construct
a the IShoe. The shoe designer's job is to create the shoe by implementing
IShoe methods. As safe and simple approach is to extend *Shoe* which implements IShoe.
You then just need to add cards to *cards* which is a *List<Card>*.

You have to make certain the IShoe implementation is in the Charlie project
class path. You do this by adding a jar or project to the IDE. Either one
works.

###Cards
There are two types of cards: *Card* and *ACard*.
The controller (i.e., Dealer) and the view (i.e., *ATable*) use
Card. Only the view uses ACard. 

Card objects have rank and suit. The following snippet constructs a three of spades:

    Card card = new Card(3, Card.Suit.SPADES)

Here's how to make an Ace of spades:

    Card card = new Card(Card.ACE, Card.Suit.SPADES)

Once you have a Card, assuming you're extending Shoe, in the _init_ method,
add it to the shoe as follows:

    cards.add(card)

Card has various methods to inquire about itself, like its value,
whether it is a face card (J, K, Q), an Ace, etc.

ACard is the "animated" analog of Card.
It is a subclass of *Sprite*. ACard objects
move around the table and have front and back faces.
They are in many way more sophisticated than a Card.
The key things to know about ACard are ACard constructs
itself from a Card, has a home position on the table, and a current position
on the table. ACard relentlessly seeks its home from its
current position by following a Euclidean straight line. This
what gives the impression of "card motion" on the table.

###IBot plugin
Named after Robot B9 from [Lost in Space](http://en.wikipedia.org/wiki/Lost_in_Space) and
Nexus 6 in [Blade Runner] (http://en.wikipedia.org/wiki/Blade_Runner),
these bots implement *IBot*, a sub-interface of IPlayer.
As far as Dealer is concerned,
B9 and N6 are players, being IPlayer instance, Dealer makes not distinction.
They run on the server which means they have special access to the server and
can crash the server if they fail. That's one reason
to be especially careful with these plugins as I explain below.

You specify B9 and N6 bots in the charlie.props file with the
keys _charlie.bot.b9_ and _charlie.bot.n6_ respectively.
The key must declare the fully qualified concrete class names.

The only difference between the
two bots is Dealer sits B9, if it is exists,
in the right seat and N6, if it exists, in
the left seat as shown below:

              DEALER
        LEFT          RIGHT
              YOU

The above symbols are in the *Seat* enumerated type.
LEFT and RIGHT are mapped to B9 and N6,  IBot instances respectively
while YOU is mapped to RealPlayer, an IPlayer.

Dealer assumes IPlayer instances (which includes
IBot instances) are an independent
threads. What does this mean?

This means an IBot must use worker threads to interact with Dealer.
IBot cannot use the thread that invoked IBot to then
invoke methods on Dealer to which the IBot has access.
For instance, IBot defines through IPlayer the behavior, _play_.
Dealer invokes _play_ on IBot only once when it is the IBot's turn.
On receiving _play_,
IBot must respond by invoking _hit_, _stay_, or _doubleDown_ on Dealer.

However, the IBot cannot invoke any of these methods on Dealer directly,
that is, not in the thread that's running
the _play_ method. The same is true for the _deal_ method
which Dealer invokes on IPlayer when it deals a card to IPlayer. To
do so would create a nasty recursion inside Dealer.

To be well-behaved,
IBot instead spawns or wakes-up a worker thread and returns to Dealer which is
waiting for a response.
The worker thread then can invoke _hit_, _stay_, or _doubleDown_ on Dealer.
Underline, double-underline, bold, put asterisks around that last sentence.

I will just point out that if IBot, in its worker thread,
invokes _hit_, Dealer may respond in turn by invoking _deal_ on IBot.
Here again, as I mentioned above, IBot must use a worker thread to respond to the
new card. I say *may* above because hit, may cause the hand to break,
Blackjack, or Charlie
in which case Dealer answers with _bust_, _blackjack, or _charlie, not _deal_.

For all practical purposes, B9 and N6 bots are identical from Dealer's point of
view. The only difference is seating as I mentioned above.
The intent of having these two bots was to also employ different play
strategies.
For instance, B9 might use the [Wizard of Odds](http://wizardofodds.com/games/blackjack/)
21 cell strategy where N6 might use the 420 cell strategy.
BTW, the Dealer does not, at the moment, support splits and that fact cuts down
on the number of cells on both cases.

###Gerty plugin
Named after the robot Gerty 3000 in [Moon](http://en.wikipedia.org/wiki/Moon_(film)),
these bots implement *IGerty* which, like IBot, is a sub-interface of IPlayer.
Unlike IBot, IGerty bots run on the client-side.
IGerty plays in place of the human player. As with the other plugins, we declare IGerty
the fully qualified concrete class name in the Charlie properties file, _charlie.props_.
The key is _charlie.bot.gerty_.

The IGerty bot has the potential to implement the most sophisticated play and bet strategies
to maximize player returns.
It might be best, for IGerty, to start with the 420 cell play strategy
and stay with a balanced, level-one system like the [Hi-Lo](http://en.wikipedia.org/wiki/Card_counting) 
or unbalanced, level-one
system like [Knock Out](http://www.koblackjack.com/).

When IGerty is playing, it must behave. Here are some rules:

1. Do not make plays before or after your turn.
2. Send one play at a time and wait for a response from Dealer.
3. Send valid plays for a hand id. For instance, do not send a double-down after the first hit request.
4. Do not send requests after you stay.
5. Wait for Dealer to compute the outcome, even if you know it before hand.
6. Don't subvert the system, e.g., look at the hole card before Dealer reveals it.
7. Play like a person, namely, add delay to decisions, otherwise, things will happen too fast and we won't be able to see or know
what it really did or why.
8. Don't place negative bets, it will just confuse Dealer.
9. Don't exit, e.g., invoke _System.exit_.
10. Don't try to initiate contact with Dealer directly. Use *Courier* which has already established
the connection to Dealer to send requests.

IGerty initiates a game by placing a bet. Here are the steps to starting:

1. Send the clear message to the table.
2. Get the wager from the table. If there isn't a wager, then use the money manager, _click_
to create one. (Note: you'll have to get the chips from the money manager and use the
chip coordinates to select the amount.
The chips are 100, 25, and 5 from left to right on the table.)
3. Invoke the *Courier* to send the bet to Dealer. The returned Hid is the hand
id for the hand. Courier is the intermediary on the client between IGerty on the client
and RealPlayer which is itself the interface to Dealer on the server.
4. Wait for _startGame_. At this point IGerty can only observe the cards until 
it is its turn.
This is an opportunity for IGerty to count cards here since every card is sent
to all players for the respective hand identified by the hand id.
6. When IGerty receives _play_, it must respond with hit, stay, or if its after the
initial deal, _doubleDown_ (see below).  IGerty sends these requests to Courier who forwards
then to RealPlayer who forwards requests to Dealer.
7. After IGerty stays, busts, gets a blackjack, or Charlie,
it must wait for _endGame_ when the game is over.
8. Go to step 1.

To play a double-down, IGerty does the following:

1. Invoke _dubble_ on the hand id. This doubles the bet in the hand.
2. Invoke _dubble_ on Courier. This send the play to the Dealer.
3. Invoke _dubble_ on the table. This doubles the wager on the table.

Of course, after double-down, IGerty is done for the game and just waits
for endGame.

###Side bet plugin
A side bet is a bet in addition to the
the main bet. The side bet usually depends on certain
card combinations.
Perhaps the most famous side bet is so-called "insurance" which
is a bet that the dealer, showing an Ace as a ten in the hole.
It pays 2:1 which is even money because you have the main bet
and the insurance premium. So if you win the side bet you also loose
the main bet.
The Basic Strategy does not recommend buying insurance.

The side bets I'm thinking about here are of the non-insurance kind, although
you might think of them as a kind of insurance.
The [Wizard of Odds](http://wizardofodds.com/games/blackjack/appendix/8/)
gives a raft of side bets from which to choose.

From a plugin perspective, there are two properties in _charlie.props_:

1. charlie.sidebet.rule
2. charlie.sidebet.view

They correspond to the Java interfaces which the properties must specify
as fully qualified classes:

1. ISideBetRule
2. ISideBetView

Dealer invokes ISideBetRule when the hand is done and reports the result to
IPlayer via the outcome.
The hand id contains the wager, both the main bet and the side bet.
For the main bet, the wager is always positive and the outcome,
win, loose, etc. determines the P&L.
In the case of Blackjack or Charlie, the odds have already been calculated in
the main bet amount in the hand id.
For instance, if the main wager is 5 and IPlayer gets a Charlie, the bet amount
in hand id is 10.

For the side bet the P&L, that is, the direction positive or negative,
is already in the side bet.
For instance, suppose the side bet is a seven on the first card.
The player makes two bets: 10 for the main bet and 5 for the side bet of seven
on the first card.
The [Wizard of Odds](http://wizardofodds.com/games/blackjack/appendix/8/) says
seven on first card pays 3:1.
But the player gets a Blackjack. Dealer pays 3:2 on the 10 and sets
the bet amount in the hand id to 15.
Dealer uses the side rule which finds no seven on first card
and the side bet rule sets -5 as the side bet.
IPlayer receives the blackjack message and adds 15 minus 5 = 10 to the IPlayer bankroll.
The table invokes _setHid_ on ISideBetView to indicate the side bet loss.

###Advisor plugin
This plugin monitors the player and when a play discrepancy is
detected, it issues a warning.
For instance, suppose we have Ace vs. 10+6. The Basic Strategy says hit.
However, if the player presses stay, IAdvisor offers advice to hit. However,
IAdvisor only needs to give the advice. IAdvisor does not have to deal with the
user interface or how to render the advice. That's the job of Charlie.

The properties files, _charlie.props_, as with the other plugins declares
the implementation of IAdvisor with the _charlie.advisor_ property. Again,
you must specify the fully qualified concrete class name.

IAdvisor receives the player's hand and the dealer's up-card. IAdvisor
has to analyze these and return a response that are in the *Play* enum:

* HIT
* STAY
* DOUBLE_DOWN
* SPLIT
* NONE

If IAdvisor cannot be loaded for some reason, the default is no advice.