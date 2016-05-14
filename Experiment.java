/* tclo, melissatan
 * 
 * Scans in experiment parameters from file.
 * 
 */

import java.util.Scanner;
import java.io.File;

public class Experiment {
    public int endTime;    //how long is the Saferide working
    public int numCall;    //num of calls Operator receives in total
    public int numPass;    //approximate num of passengers generated in total
    public double arrRate;
    public double passPerCall;
    public int numVan;
    public int vanCap;
    public Address startAddress;
    
    public Experiment(String f) {
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File(f), "UTF-8");
        } catch (NullPointerException e) {
            System.out.print("Bad file name.");
            System.exit(0);
        } catch (java.io.FileNotFoundException e) {
            System.out.println("File " + f + " not found.");
            System.exit(0);
        }

        endTime = scanner.nextInt();
        numCall = scanner.nextInt();
        numPass = scanner.nextInt();
        arrRate = ((double) numCall) / endTime;
        passPerCall = numPass *1.0 / numCall;
        numVan = scanner.nextInt();
        vanCap = scanner.nextInt();
        startAddress = new Address(scanner.next(), scanner.next());
    }
}


