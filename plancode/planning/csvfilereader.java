package planning;
import java.io.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
/** This parses csv files meeting the official UK standard for such files at https://www.ofgem.gov.uk/sites/default/files/docs/2013/01/csvfileformatspecif(ication.pdf
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
   along with this program.  If not, see <https://www.gnu.org/licenses/>.
   * */

public
class csvfilereader {

    static final int textlen=80;

    public static void main(String [] args) {
        if (args.length<1) {
            System.out.println(" Usage java csvfilereader file.cvs");
        } else {
            csvfilereader r= new csvfilereader(args[0]);
            pcsv p=r.parsecsvfile();
            if(p==null) {
                System.out.println(" null returned from parse ");
            } else {
                System.out.println(p);
                double[][] d =  r.getdatamatrix(p);
                System.out.println(Arrays.deepToString(d));
            }
        }
    }
    /** returns null for file that can not be opened, otherwise
     returns pointer to tree of csvcells. */


    static final int FD=34, /* field delimitor */
                     FS=44, /* field separator */
                     RS=10,  /* record separator */
                     EOI=0x1a,
                     CR=13;
    enum token {FDsym,FSsym,RSsym,EOFsym,space,any};

    token[] categorisor=new token[128] ;
    void recurse(int j,int i,pcsv q,double[][] m) {
        if( q != null ) {
            if( i>=1 ) {
                if( q .tag instanceof numeric ) {
                    m [j][i] = ((numeric)q.tag) .number;
                } else {
                    m [j][i] = 0.0;
                }
            }
            recurse(j,i+1,q .right,m);
        }
        ;
    };
    void recursedown(int j, pcsv q, double [][]m) {
        if( q != null ) {
            recurse (j,0,q .right,m);
            recursedown(j+1,((linestart)q.tag) .down,m);
        }
    };
    double[][] getdatamatrix(pcsv p)
    /** extract the data as matrix of doubles */
    {
        double [][]m;
        if( p== null ) {
            return null ;
        } else {
            m= new double[rowcount(p)][colcount(p)];
            recursedown (1,((linestart)p.tag) .down,m);
            return m;
        }
    };
    void recursegch(int i, pcsv q, String[] h ) {
        if( q != null ) {
            //	System.out.println("recursegch("+i+","+q+","+Arrays.toString(h));
            if(q.tag instanceof linestart) {
                h[i]="";
            } else {
                h [i] = q.tag.toString() ;
            }
            recursegch(i+1,q .right,h);
        }
    };
    public  String[] getcolheaders( pcsv p) {
        /** extract the column headers */

        String[] h ;
        if( p== null ) {
            return null;
        } else {
            h= new String[colcount(p)];
            recursegch (0,p .right,h);
            return h;
        }
    }
    void recursegrh(int i, pcsv q, String[]h) {
        if( q != null ) {
            if(q.right !=null)h [i] =""+ q .right.tag ;
            recursegrh(i+1,((linestart)q .tag).down,h);
        }
    }
    String[] getrowheaders(pcsv p ) {
        /** extract the rows headers */
        int M,i ;
        String[] h ;
        if( p== null ) {
            return null;
        } else {
            h= new String[ rowcount(p)];
            recursegrh (0,p,h);
            return h;
        }
    };
    int colcount(pcsv p )
    /** return the number of columns in the spreadsheet */
    {
        if( p == null ) {
            return 0;
        } else {
            if( p .tag instanceof      linestart    )    return colcount(p .right);
            if (p.tag instanceof numeric || p.tag instanceof alpha)return 1+colcount(p .right);
        }
        return 0;
    }

    pcsv getcell(pcsv p,int row, int col )
    /** return the cell at position row,col in the spredsheet*/
    {
        if( p== null ) {
            return null;
        } else if( row==1 ) {
            if( p .tag instanceof linestart ) {
                return getcell (p .right,row,col);
            } else if( col == 1 ) {
                return p;
            } else {
                return getcell(p .right,row,col-1);
            }
        } else {
            return getcell(((linestart)(p.tag)) .down,row-1,col);
        }
    };
    boolean onlynulls(pcsv q ) {
        if( q== null ) return false;
        else if( q .tag instanceof alpha ) {
            return ( q .right== null ) && (((alpha)(q.tag)).textual =="");
        } else return false;
    };
    void removetrailingnull( pcsv p ) {
        if( p != null ) {
            if( p .tag instanceof linestart) {
                if( ((((linestart)(p.tag)) .down== null ) && onlynulls(p .right)) ) p.right = null;
                else removetrailingnull(((linestart)(p.tag)) .down);
            }
        }
    }
    int rowcount(pcsv p) {
        if( p == null ) return 0;
        else if( p .tag instanceof linestart)return 1+rowcount(((linestart)(p.tag)) .down);
        if (p.tag instanceof numeric|| p.tag instanceof alpha)return 1;
        return 0;
    }
    boolean isint(double r ) {
        long i;
        i = (long)(r);
        return (i*1.0)==r;
    }
    void printcsv(PrintWriter f,pcsv p ) {
        if( p != null ) {
            if( p.tag instanceof linestart ) {
                printcsv(f,p.right);
                if(((linestart)(p.tag)).down != null ) {
                    f.println();
                    printcsv(f,((linestart)(p.tag)).down);
                };
            } else if( p.tag instanceof numeric ) {
                if( isint(((numeric)(p.tag)).number) ) f.print((long)(((numeric)(p.tag)).number) );
                else f.print( ((numeric)(p.tag)).number );
                if(p. right != null ) {
                    f.print(",");
                    printcsv(f,p.right);
                }
            } else if( p.tag instanceof alpha ) {
                if( ((alpha)(p.tag)).textual != null ) f.print("\""+((alpha)(p.tag)).textual+"\"") ;
                else f.print(" null ");
                if( p.right != null ) {
                    f.print( ",");
                    printcsv(f,p.right);
                }
            }
        }
    }

    token thetoken() {
        if( currentchar < bp.length )
            return categorisor[bp [currentchar]];
        else return token.EOFsym;
    };
    boolean peek( token c)
    /** matches current char against the token c returns true if it matches. */
    {
        return c==thetoken();
    }
    boolean isoneof ( EnumSet<token>s) {
        return s.contains( thetoken ());
    };
    void nextsymbol() {
        if( currentchar < bp.length ) currentchar = currentchar+1;
    };
    boolean have( token c) {
        if( peek(c) ) {
            nextsymbol();
            return true;
        } else
            return false;
    }
    boolean haveoneof( EnumSet<token>c) {
        if( isoneof(c) ) {
            nextsymbol();
            return true;
        } else
            return false;
    };

    void initialise() {
        firstfield = null ;
        lastfield = null ;
        firstrecord = null ;
    };
    int min(int a, int b) {
        return (a<b?a:b);
    }
    void resolvealpha() {
        int i,l;
        {
            lastfield.tag =    new alpha("");
            l = min( tokend, (tokstart +textlen-1));
            /* copy field to string*/
            alpha p = (alpha)lastfield.tag;
            for( i = tokstart ; i<=( l -1); i++) {
                p.textual = p.textual + (char)(bp [i]);
            };
        };
    };

    void resolvedigits() {
        int i,l ;
        String s;
        // System.out.println("resolvedigits");
        {
            lastfield.tag = new numeric();
            numeric n= (numeric)lastfield.tag;
            s = "";
            l =min( tokend, (tokstart +textlen-1));
            // System.out.println("l="+l);
            /* copy field to a string */
            for( i = tokstart; i< l; i++) {
                s = s + (char)(bp [i]);
                //System.out.println("s="+s+" i="+i+" bp[i]="+bp[i]);
            }
            n.number = Double.valueOf(s );/* convert to binary*/
        };
    };
    char chr(int i) {
        return (char)i;
    }
    void resolvetoken() {
        if( chr(bp [tokstart]) >='0' &&chr(bp [tokstart])<= '9' ) resolvedigits();
        else resolvealpha();
    };

    void markbegin() { /* mark start of a field */
        tokstart = currentchar;
        lastfield .right=new pcsv();
        lastfield = lastfield .right;
        lastfield .right = null ;
    };
    void markend() { /* marks the endof a field */
        tokend = currentchar;

        resolvetoken();
    };
    void setalpha( String s ) {
        lastfield .tag = new alpha(s);
    };
    void emptyfield() {
        markbegin();
        setalpha("");
    };

    void parsebarefield() {
        EnumSet<token>s= EnumSet.of( token.RSsym,token. EOFsym,token. FSsym);
        if( isoneof(s) ) emptyfield();
        else {
            markbegin();
            s=EnumSet.of(token.any,token.space);
            while(haveoneof(s)) {}; /* skip over the field */
            markend();
        };
    };
    void appendcurrentchar() {
        parsedelimitedfields = parsedelimitedfields+chr(bp [currentchar]);
        nextsymbol();
    };
    String parsedelimitedfields;
    int length(String s) {
        return s.length();
    }
    void parsedelimitedfield() {
        /** parses a field nested between " chars converting escape chars as it goes */
        int i;
        boolean docontinue;
        EnumSet<token>s= EnumSet.range(token.FSsym,token.any);
        markbegin();
        parsedelimitedfields = "";
        docontinue = true;
        do {
            while(isoneof(s)) {
                appendcurrentchar();
            };
            have(token.FDsym);/* eat what may be closing quotes*/
            docontinue = peek(token.FDsym) && (length (parsedelimitedfields) < textlen);
            if( docontinue ) appendcurrentchar();
        } while ( docontinue) ;
        setalpha( parsedelimitedfields);
    }
    void parsefield() {
        if( have(token.FDsym) ) parsedelimitedfield();
        else parsebarefield();
    };
    void parserecord() {
        parsefield();
        while(have (token.FSsym)) parsefield();
    };
    void parseheader() {
        /* claim heap space for start of first line */
        firstrecord= new pcsv();
        lastfield = firstrecord;
        firstfield = firstrecord;
        firstrecord. tag = new linestart(null);
        firstrecord . right = null ;
        parserecord();

    };
    void parsewholefile() {
        parseheader();
        while( have(token.RSsym)) {

            /* claim heap space for the start of the new line */
            linestart l = (linestart)firstfield . tag;
            l.down = new pcsv();
            firstfield = l .down;
            lastfield = firstfield;
            firstfield. tag = new linestart(null);
            firstfield. right = null ;
            parserecord();

        };
    }
    static final int
    megabyte =1024*1024,
    maxbuf = 30*megabyte;


    byte [] bp;
    int fs,rc,
        tokstart,tokend,currentchar ;
    pcsv firstfield,lastfield,firstrecord;
    String theFilename;
    pcsv parsecsvfile(  ) {
        initialise();
        /* open file for reading*/
        try {
            Path path = Paths.get(theFilename);
            byte[] data = Files.readAllBytes(path);
            bp=data;
            currentchar = 0;
            /** We now have the csv file in memory - parse it */
            parsewholefile();
            removetrailingnull(firstrecord);
            return firstrecord;
        } catch(Exception e) {
            return null;
        }
    }
    public csvfilereader(String fn) {
        int i;
        theFilename=fn;
        for (i=0; i<128; i++)categorisor[i] = token. any;
        categorisor[FD] = token. FDsym;
        categorisor[FS] = token. FSsym;
        categorisor[RS] = token. RSsym;
        categorisor[EOI] = token. EOFsym;
        categorisor[(int)(' ')] = token.space;
        categorisor[CR] = token.space;
    }



}
