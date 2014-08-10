ExtensiveFormGames
==================

Contains code for modeling two-player zero-sum extensive-form games and algorithms for computing Nash equilibria and related concepts. Right now, the file reader can read two formats, the one used by the [zerosum](http://www.cs.cmu.edu/~./waugh/zerosum.html) package, and an extension of that format, allowing heavier annotation of the game. In the future, I will hopefully add a formal description of the two formats.

If you use this codebase, please cite one of our recent papers, for example:

Extensive-form Game Abstraction with Bounds. Christian Kroer and Tuomas Sandholm. In _ACM conference on Economics and Computation_ (EC), 2014

For a C library implementation of the sequence form linear program and the counter-factual regret minimization algorithm, check out Kevin Waugh's [zerosum](http://www.cs.cmu.edu/~./waugh/zerosum.html) package.

Dependencies
============

One or more solvers in this library require CPLEX to run. In order to use these, you must add [cplex.jar](http://www-01.ibm.com/software/commerce/optimization/cplex-optimizer/) to `lib/` to allow compilation. To run it, add the following as a VM argument:

-Djava.library.path=/path/to/CPLEX_Studio/cplex/bin/your-architecture/

To run the JUnit test cases, the variables in the `TestConfiguration` class should be appropriately updated.

References
==========

`SequenceFormLPSolver` is an implementation of the sequence form linear program described in:
Efficient Computation of Behavior Strategies. Bernhard von Stengel. In _Games and Economic Behavior_, 1996


