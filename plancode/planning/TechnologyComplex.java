package planning;
import java.io.*;
import java.util.*;
/** A class to represent a set of production technologies in a more compact
  form than as an input output table or matrix. It can take advantage
  of the sparse character of large io tables.
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
public class TechnologyComplex {

    public Vector< Technique > techniques= new Vector<Technique>();
    public String[] productIds;
    public boolean[] nonproduced;
    Vector< Vector<Integer>> producerIndex =null;
    Vector< Vector<Integer>> userIndex =null;
    public boolean[] nonfinal;
    /** return a vector indexed by product each of whose elements is a vector of indices of techniques that use the product */
    public Vector< Vector<Integer> > buildUserIndex() {
        if(userIndex==null) {
            Vector<Vector<Integer> > index = new Vector< Vector<Integer>>( );
            for (int i=0; i<productCount(); i++)
                index.add(new Vector<Integer>());

            for (int i =0; i<techniqueCount(); i++) {
                Technique t= techniques.elementAt(i);
                for (int j=0; j<t.inputCodes.length; j++) {
                    index.elementAt(t.inputCodes[j]).add(new Integer(i));
                }

            }
            userIndex=index;
            return index;
        }
        return userIndex;
    }
    /** return a vector indexed by product each of whose elements is a vector of  indices of techniques that make that product */
    public Vector< Vector<Integer> > buildProducerIndex() {
        if(producerIndex==null) {
            Vector<Vector<Integer> > index = new Vector< Vector<Integer>>( );
            for (int i=0; i<productCount(); i++)
                index.add(new Vector<Integer>());
            for (int i =0; i<techniqueCount(); i++) {
                Technique t= techniques.elementAt(i);
                index.elementAt(t.productCode).add(new Integer(i));
            }
            producerIndex=index;
            return index;
        }
        return producerIndex;
    }
    public TechnologyComplex(int NumberOfProducts) {

        productIds= new String[NumberOfProducts];
        nonproduced=new boolean[NumberOfProducts];
        nonfinal=new boolean[NumberOfProducts];
    }
    public void addTechnique(Technique t) {
        techniques.add(t);
    }
    public void setProductName(int productCode,String productName) {
        productIds[productCode]=productName;
    }
    public int productCount() {
        return productIds.length;
    }
    public int techniqueCount() {
        return techniques.size();
    }
}
