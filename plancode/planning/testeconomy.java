package planning;
import java.io.*;
import java.util.*;
/** this generates test economies of varying sizes for testing
  *  the planning software
  *  Usage : java planing.testeconomy <number of sectors> */
class testeconomy {

    static int sectors=1;
    static Random r=new Random();
    static double depr=0.07;
    static float [] netuse;
    static final double ln2 =Math.log(2);
    static int ranlogn(int sectors) {
        double l2of=Math.log(sectors)/ln2;
        if (r.nextInt(sectors)<(l2of+5)) return r.nextInt();
        return 0;

    }
    public static void main(String[] args) {
        int row,col,totl;
        String [] heads;
        if(args.length!= 2) {
            System.err.println("Usage : java planing.testeconomy <number of sectors> <years>");
        }
        sectors = new Integer(args[0]).intValue();
        int years=new Integer(args[1]).intValue();
        netuse=new float[sectors];
        heads=headings(sectors);

        PrintStream flows,cap,dep,targ;
        try {
            flows=new PrintStream("testflow.csv");
            prnhead(heads,flows);
            cap=new PrintStream("testcap.csv");
            prnhead(heads,cap);
            dep=new PrintStream("testdep.csv");
            prnhead(heads,dep);
            String[] targh=new String[sectors+1];
            for(col=0; col<sectors; col++)targh[col]=heads[col];
            targh[sectors]="labour";
            targ = new PrintStream("testtarg.csv");
            prnhead(targh,targ);

            for(row=0; row<sectors; row++) {
                cap.print(heading(row));
                dep.print(heading(row));
                flows.print(heading(row));
                netuse[row]=0;
                for (col=0; col<sectors; col++) {
                    dep.print(","+depr);
                    int R= ranlogn(sectors);
                    if((R & 1)==1) {
                        flows.print(","+(R&3));
                        netuse[row]+= (R&3);
                    } else {
                        flows.print(",0");
                    }
                    if((R & 2)==2) {
                        cap.print(","+(R&7));
                        netuse[row]+= ((R&7)*depr);
                    } else {
                        cap.print(",0");
                    }
                }
                cap.println("");
                flows.println("");
                dep.println("");
            }
            flows.print("labour");
            totl=0;
            for(col=0; col<sectors; col++) {
                int L= r.nextInt(9)+1;
                totl+=L;
                flows.print(","+L);
            }
            flows.println("");
            flows.print("output");

            for(col=0; col<sectors; col++) {

                flows.print(","+(1+2*((int)netuse[col])));
            }
            flows.println("");
            double scale =0.9;
            for(int year=1; year<=years; year++) {
                targ.print("year"+year);
                for(col=0; col<sectors; col++) {
                    targ.print(","+(0.01+ (scale*netuse[col])));
                    netuse[col]*=1.03;
                }
                targ.println(","+totl);


                totl=(int)(totl*1.02);
            }
        } catch( FileNotFoundException nf) {
            System.err.println("Error "+nf);
            System.exit(0);
        }
    }
    static String[] headings(int sectors) {
        String[] h= new String[sectors];
        for(int i=0; i<sectors; i++)h[i]=heading(i);
        return h;
    }
    static String heading(int n) {
        return "P"+n;
    }
    static void prnhead(String[]h,PrintStream s) {
        int i;
        s.print("headings");
        for(i=0; i<h.length; i++)s.print(","+h[i]);
        s.println("");
    }
}
