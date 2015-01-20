# BallistcDiffusionModel
*A software application enabling the simulation and analysis of mathematical and physical models. This version feature the use of the software to simulate atomic surface deposition, but the software may easily be abstracted to run other simulations.*

----
## Simulation of Large Systems
The accuracy of Monte Carlo simulations improves significantly as the size of the system is increased. However, finite RAM limits the size of systems that can be stored in memory. To overcome such limitations, this software introduces a data structure called the EmbeddedDBArray. The EmbeddedDBArray consists of an array stored in memory and an embedded database of a size much greater than that of the array. Records are stored in the database, and checked out into the "working copy", or array in memory. All changes to the array are then pushed when a different section of the records are accessed. The size of the array in this implementation varies from ~17 to 67 million, similar to the maximum size of an array in java. This data structure was tested successfully for a total dataset size of 1 billion, for which an iterations worth of push and pull operations took several hours.

Further space complexity reduction in the lattice representation of the surface enables the analysis of still larger systems. In surface deposition, the algorithm to deposit particles and the analytical quantities of interest are primarily dependent on the uppermost region of the deposition. Thus it is unnecessary to store the entirety of the surface as it grows higher and higher. Instead, the LargeSystemDeposition in this model grows within a fixed 2D square lattice of a height much smaller than the total height of the deposition.  This lattice is called a "slot". When the slot is filled to the brim for the first time, the entire bottom half is cleared.  The deposition continues to grow freely in the bottom half of the slot.  Once the height of the deposition reaches the middle of the slot, the top half is cleared and the deposition continues to grow. The growth process may repeat indefinitely with no additional space complexity introduced.

----
## Data Storage and Analysis
To enable simple access, updates and storage of data, this software provides a MySQLClient built upon the java JDBC platform. This feature makes the AnalysisControl tool possible.  In addition to storing quantities of interest after each simulation, the model also stores average values for specific quantities at different points in time during model execution. This enables time-lapse figures such as the Scaled average width plot. The DataManager class also saves models to text and csv files for visual perusal and manipulation in Excel.

----
## References
*The visualizations and user interfaces were built in part using the [OpenSourcePhysics](http://www.opensourcephysics.org/webdocs/programming.cfm?t=Overview) framework.*
