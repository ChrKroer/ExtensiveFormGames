ExtensiveFormGames
==================

Contains code for modeling two-player zero-sum extensive-form games and algorithms for computing Nash equilibria.

Dependencies
============

One or more solvers in this library require CPLEX to run. In order to use these, you must add [cplex.jar](http://www-01.ibm.com/software/commerce/optimization/cplex-optimizer/) to `lib/` to allow compilation. To run it, add the following as a VM argument:

-Djava.library.path=/path/to/CPLEX_Studio/cplex/bin/your-architecture/

