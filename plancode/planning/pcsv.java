package planning;
public class pcsv {
    pcsv right;
    celltype tag;
    public String toString() {
        return tag.toString()+
               (right==null?"":
                (tag instanceof linestart?"":",")
                +right.toString())+
               (tag instanceof linestart?
                (
                    ((linestart)tag).down==null?"":
                    ""+((linestart)tag).down):"");
    }
};
class celltype {};
class linestart extends celltype {
    pcsv down;
    linestart(pcsv p) {
        down=p;
    }
    public String toString() {
        return "\n";
    }
};
class numeric extends celltype {
    double number;
    public String toString() {
        return ""+number;
    }
};

class alpha extends celltype {
    String textual;
    public String toString() {
        return textual;
    }
    alpha(String s) {
        textual =s;
    }
};
