package planning;
import java.io.*;
import java.util.*;
/** A class to compute the harmony function described in Towards a New Socialism
  <p>
       Copyright (C) 2018 William Paul Cockshott

       This program is free software: you can redistribute it and/or modify
       it under the terms of the GNU General Public License as published by
       the Free Software Foundation, either version 3 of the License, or
       (at your option) any later version.

       This program is distributed in the hope that it will be useful,
       but WITHOUT ANY WARRANTY; without even the implied warranty of
       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
       GNU General Public License for more details.

       You should have received a copy of the GNU General Public License
       along with this program.  If not, see https://www.gnu.org/licenses/.
    * */
public class Harmony {

    /** the harmony function itself */
    public static double H(double target, double netoutput) {

        double scale =  (netoutput-target  )/target;
        //    System.out.println("H("+target+","+netoutput+") scale "+scale);
        if (scale<0) return scale - (scale*scale)*0.5;
        return Math.log(scale+1);


    }
    /** the derivative of the harmony function
     * evaluated numerically so as to be independent of the H function */
    public static double dH(double target, double netoutput) {

        double epsilon = 0.000001;
        double base = H(target,netoutput);
        double basePlusEpsilon = H(target, epsilon+netoutput);
        return (basePlusEpsilon - base)/epsilon;
        // Analytic soln
        //  double scale =  (netoutput-target  )/target;
        //  if (scale<0) return 1-scale;
        //  return 1/(1+scale);
    }
}
