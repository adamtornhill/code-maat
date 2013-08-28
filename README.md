# Code Maat

Code Maat is a command line tool used to mine and analyze data from version-control systems (VCS).
Currently, I'm not hosting any pre-built binaries. Code Maat is written in Clojure. To build it from source, use [leiningen](https://github.com/technomancy/leiningen):

	   lein uberjar

The command above will create a standalone jar containing all the dependencies.

## The ideas behind Code Maat

To understand large-scale software systems we need to look at their evolution. The history of our system provides us with data we cannot derive from a single snapshot of the source code. Instead VCS data blends technical, social and organizational information along a temporal axis that let us map out our interaction patterns in the code. Analyzing these patterns gives us early warnings on potential design issues and development bottlenecks, as well as suggesting new modularities based on actual interactions with the code. Addressing these issues saves costs, simplifies maintenance and let us evolve our systems in the direction of how we actually work with the code.

Code Maat was developed to accompany the discussions in my book [Code as a Crime Scene](https://leanpub.com/crimescene).

### About the name

Maat was a goddess in ancient Egyptian myth. She was the one who gave us order out of the initial chaos in the universe. Code Maat hopes to continue the work of Maat, albeit on a smaller basis, by highlighting code with chaotic development practices and suggest the directions of future refactorings to bring order to it. Further, maat was used in ancient Egypt as a concept of truth. And metrics never lie (except when they do).

## Usage

Code Maat operates on logfiles from version-control systems.

### Generating input data

#### Analyzing Subversion data

#### Generate a Subversion logfile using the following command:

          svn log -v --xml > logfile.log

#### Analyzing git data

* Git is not supported in this early release. Stay tuned for an update that will support git.

### Running Code Maat

You can run Code Maat directly from leiningen:

    	  lein run logfile.log

If you've built a standalone jar (`lein uberjar`), run it with a simple java invocation:

     	  java -jar code-maat-0.1.0.jar

When invoked without any arguments, Code Maat prints its usage:

             adam$ java -jar code-maat-0.1.0.jar
             Switches           Default  Desc                                                                    
             --------           -------  ----                                                                  
             -a, --analysis     authors  The analysis to run (authors, revisions, coupling, :all)                
             -r, --rows         10       Max rows in output                                                      
             -e, --max-entries  1000     Max entries to parse in the input log file                              
             -d, --date                  The start date to consider in the logs, given as yyyyMMdd               
             --min-revs         5        Minimum number of revisions to include an entity in the analysis        
             --min-shared-revs  5        Minimum number of shared revisions to include an entity in the analysis 
             --min-coupling     50       Minimum degree of coupling (in percentage) to consider  

#### Mining organizational metrics

By default an analysis on the number of authors per module is run. The authors analysis is based on the idea that the more developers working on a module, the larger the communication challenges. The analysis is invoked with the following command:

   	   java -jar code-maat-0.1.0.jar logfile.log

The resulting output is on CSV format:

              entity,         n-authors, n-revs
              InfoUtils.java, 12,        60
              BarChart.java,   7,        30
              Page.java,       4,        27
              ...

In example above, the first column gives us the name of module, the second the total number of distinct authors that have made commits on that module, and the third column gives us the total number of revisions of the module. Taken together, these metrics serve as predictors of defects and quality issues.

#### Mining logical coupling

Logical coupling refers to modules that tend to change together. Modules that are logically coupled have a hidden, implicit dependency between them such that a change to one of them leads to a predictable change in the coupled module. To analyze the logical coupling in a system, invoke Code Maat with the following arguments:

              java -jar code-maat-0.1.0.jar logfile.log -a coupling

The resulting output is on CSV format:

              entity,          coupled,        degree,  average-revs
              InfoUtils.java,  Page.java,      78,      44
              InfoUtils.java,  BarChart.java,  62,      45
              ...

In the example above, the first column (`entity`) gives us the name of the module, the second (`coupled`) gives us the name of a logically coupled module, the third column (`degree`) gives us the coupling as a percentage (0-100), and finally `average-revs` gives us the average number of revisions of the two modules. To interpret the data, consider the `InfoUtils.java` module in the example output above. The coupling tells us that each time it's modified, it's a 78% risk/chance that we'll have to change our `Page.java` module too. Since there's probably no reason they should change together, the analysis points to a part of the code worth investigating as a potential target for a future refactoring.

### Limiting the analysis to a temporal period

To analyze our VCS data we need to define a temporal period of interest. Over time, many design issues do get fixed and we don't want old data to infer with our current analysis of the code. To limit the data Code Maat will consider, provide a `--date` argument on the format `yyMMdd`:

   	   java -jar code-maat-0.1.0.jar logfile.log -a coupling --date 20130820

### Visualizing the result

Future versions of Code Maat are likely to include direct visualization support. For now, the generated csv can be saved to a file and imported into a spreadsheet program such as OpenOffice or Excel. That allows us to generate charts such as the ones below:

![coupling visualized](doc/imgs/coupling_sample.png).

### JVM options

Code Maat uses the Incanter library. By default, Incanter will create an `awt frame`. You can surpress the frame by providing the following option to your `java` command: `-Djava.awt.headless=true`.

## Limitations

The current version of Code Maat processes all its content in memory. Thus, it doesn't scale to large input files. The recommendation is to limit the input by specifying a sensible start date with `--date` (you want to do that anyway to avoid confounds in the analysis). Further, you can modify the `--max-entries` switch to control the number of entries considered in the log.
In a future version of Code Maat I plan to support a database back end.

## Future directions

In future versions of Code Maat I plan to support `git`. I'll also probably add more analysis methods such as code churn and developer patterns.

## License

Copyright © 2013 Adam Tornhill

Distributed under the [GNU General Public License v3.0](http://www.gnu.org/licenses/gpl.html).
