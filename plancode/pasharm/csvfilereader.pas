unit csvfilereader;
(*! This parses csv files meeting the official UK standard for such files The following text is imported from that
definition at https://www.ofgem.gov.uk/sites/default/files/docs/2013/01/csvfileformatspecification.pdf

\section{Introduction} 
\subsection{Background}
The  comma
separated  values  (CSV)  format  is  a  widely  used  text  file  format  often  used  to  exchange
data  between  applications.  It  contains  multiple  records  (one  per  line),  and  each  field  is  delimited  by  a
comma.

\subsection{CSV File Format}
The primary function
of CSV file
is to
separate each field values by comma separated and
transport
text
-
based data
to one or more target application. A source application is one which creates or appends
to a CSV file
and a
target application is one which reads a CSV file
\subsubsection{CSV File Structure}
The
CSV file structure use following
two
notations

FS (Field
Separator) i.e. comma separated

FD (Field Delimiter) i.e.  Always use a double
-
quote.

Each line feed in CSV file represents one record and each line is terminated by any valid
NL (New line
i.e. Carriage Return (CR)
ASCII (13)
and Line Feed (LF)
ASCII (10)
) feed.
Each
record
contains
one or more
fields and the fields are separated by the
FS character (i.e. Comma)
A field
is a string of text characters
which will be delimited
by the FD character (i.e. double
-
quote (''))
Any field may be quoted (with double quotes).


Fields containing a line
-
break, double
-
quote, and/or commas should be quoted. (If they are not, the file
will likely be impossible to process correctly).


The FS ch
aracter (i.e. comma) may appear in a FD delimited field and in this case it is not treated as
the field separator.
If a field's
value
contains one or more commas, double
-
quotes, CR or LF characters, then it MUST be
delimited by a pair of double
-
quotes (AS
CII 0x22).


DO NOT apply double
-
quote protection where it is not required as applying double quotes on every field
or on empty field
would
takes more file space
If a field requires Excel protection, its
value
MUST be prefixed with a single tilde character
.

See example below:

FS =,

FD ="

Data Record:{\tt

Test1,Test2,,"Test3,Test4","Test5 ""Test6"" Test7","Test8,""",",Test9"}


Indicat
es the following four fields

\begin{tabular}{ll}
{\tt Test1}&
5
characters\\
{\tt Test2}&
5
characters\\
&
0 characters
\\
{\tt Test3,Test4}  &
11 characters
\\
{\tt Test5 "Test6" Test7}
&20  characters
\\
{\tt
Test8,"}
&8 characters
\\
{\tt ,Test9}
&6 characters
\end{tabular}
\section{
CSV File Rules}\begin{itemize}\item
The file type extension MUST be set to .CSV
\item
The character set used by data contained in the file MUST be an 8
-
bit (UTF
-
8).
\item
No binary data should be transported in CSV file.
\item
A CSV file MUST contain at least one record.
\item
No limit to the number of data records
\item
The End of Record m
ust be set to
CR
+LF
(i.e. Carriage Return
and Line Feed
)
\item
Do not use whitespaces in the file name
\item
The EOR marker MUST NOT be taken as being part of the CSV record
\item
EOF  (End  of  File)
character  indicates  a  logical  EOF  (SUB
-
ASCII  0x1A)  and  not  the  physical
en
d
.
\item
A logical EOF marker cannot be double
-
quote protected.
\item
Any record appears after the EOF will be ignored
\end{itemize}
\subsection{
File Size}
Maximum csv file size
should be 30 MB.
\subsection{CSV Records}
A CSV record consists of two elements, a
data record
followed by an end
-
of
-
record marker (EOR). The
EOR is a
data record
delivery marker and does not form part of the data delivered by the record
\section{
CSV Record Rules}
Pls. note this rule applies to every CSV record including the last record in the file.
\subsection{
CSV Field Column Rules}\begin{itemize}
\item
Each  recor
d  within  the  same  CSV  file  MUST  contain  the  same  number  of  field  columns
.  The
header record describes how many fields the application should expect to process.
\item
Field columns MUST be separated from each other by a single separation character
\item
A field column
MUST NOT have leading or trailing whitespace\end{itemize}
\subsection{Header Record Rules}
A header record allows the Ofgem IT systems to guard against the potential issues such as missing
column or additional column that are not in scope\begin{itemize}
\item
The header record MUST be the first recor
d in the file.
\item
A CSV file MUST contain one header record
only
.
\item
Header labels MUST NOT be blank.
\item
Use single word only\item
Do not use spaces (Use \_ if words needs to be separated)
\end{itemize}*)
interface
const textlen=80;
type pcsv=^csvcell;
     celltype=(linestart,numeric,alpha);
     textfield=textline;
     csvcell = record
		right:pcsv;
		case tag:celltype of
		linestart:(down:pcsv);
		numeric : (number:real;);
		alpha   : (textual:pstring;);
		end;

     headervec(max:integer) =array [1..max]of pcsv;
     pheadervec=^headervec;
procedure printcsv(var f:text;p:pcsv);
function parsecsvfile(name:textline):pcsv;
function rowcount(p:pcsv):integer;
function getdatamatrix(p:pcsv):^matrix;
function getcell(p:pcsv;row,col:integer):pcsv;
function getrowheaders(p:pcsv):^headervec;
function getcolheaders(p:pcsv):^headervec;
function colcount(p:pcsv):integer;
(*! returns nil for file that can not be opened, otherwise
    returns pointer to tree of csvcells. *)
implementation

const FD=34; {field delimitor }
      FS=44; {field separator }
      RS=10;       {record separator }
      EOI=$1a;
      CR=13;
type token =(FDsym,FSsym,RSsym,EOFsym,space,any);
     tokenset = set of token;
var categorisor:array[byte] of token;

function getdatamatrix(p:pcsv):^matrix;
(*! extract the column headers as a vector of strings *)
var  m:^matrix;
    procedure recursedown(j:integer;q:pcsv);
	    procedure recurse(i:integer;q:pcsv);
	    begin

			if q<>nil then
			begin
			    if i>=1 then
			    begin
				    if q^.tag = numeric then
		 			m^[j,i]:=q^.number
					else m^[j,i]:=0.0
				end;
				recurse(i+1,q^.right);
			end
			 ;
		end;

    begin
		if q<>nil then
		begin
			recurse (0,q^.right);
			recursedown(j+1,q^.down);
		end
	end;
begin

    if p=nil then getdatamatrix:=nil
    else
    begin
	new(m,rowcount(p)-1,colcount(p)-1);
	recursedown (1,p^.down);
	getdatamatrix:=m;
	end;
end;
function getcolheaders(p:pcsv):^headervec;
(*! extract the column headers  *)
var M,i:integer;h:^headervec;
    procedure recurse(i:integer;q:pcsv);
    begin
		if q<>nil then
		begin

			if i>=1 then h^[i]:=q ;
			recurse(i+1,q^.right);
		end
	end;
begin

    if p=nil then getcolheaders:=nil
    else
    begin
	new(h,colcount(p)-1);
	recurse (0,p^.right);
	getcolheaders:=h;
	end;
end;
function getrowheaders(p:pcsv):^headervec;
(*! extract the rows headers *)
var M,i:integer;h:^headervec;
    procedure recurse(i:integer;q:pcsv);
    begin
		if q<>nil then
		begin
			h^[i]:=q^.right;
			recurse(i+1,q^.down);
		end
	end;
begin
    if p=nil then getrowheaders:=nil
    else
    begin
	new(h,rowcount(p)-1);
	recurse (1,p^.down);
	getrowheaders:=h;
	end;
end;
function colcount(p:pcsv):integer;
(*! return the number of columns in the spreadsheet *)
begin
	if p = nil then colcount:=0
	else
	case p^.tag of
	linestart: colcount:=colcount(p^.right);
	numeric,alpha:colcount:=1+colcount(p^.right)
	end
end;
function getcell(p:pcsv;row,col:integer):pcsv;
(*! return the cell at position row,col in the spredsheet*)
begin
  if p=nil then getcell:=nil
  else if row=1 then
  begin
	if p^.tag = linestart then getcell:=getcell (p^.right,row,col)
	else if col = 1 then getcell:=p
	else getcell:=getcell(p^.right,row,col-1)
  end
  else getcell:=getcell(p^.down,row-1,col)
end;

procedure removetrailingnull( var p:pcsv);
  function onlynulls(q:pcsv):boolean;
  begin
	if q=nil then onlynulls:=false
	else
	 if q^.tag=alpha then
	     begin
	       onlynulls:=( q^.right=nil) and (q^.textual^='')
	     end
	      else onlynulls:=false
   end;
begin
	if p <> nil then
	case p^.tag of
	linestart:
	    if ((p^.right=nil) and (p^.down=nil))
	    or((p^.down=nil)and onlynulls(p^.right)) then p:=nil
	    else removetrailingnull(p^.down);
	end
end;
function rowcount(p:pcsv):integer;
begin
	if p = nil then rowcount:=0
	else
	case p^.tag of
	linestart: rowcount:=1+rowcount(p^.down);
	numeric,alpha:rowcount:=1
	end
end;
function isint(r:real):boolean;
var i:integer;
begin
	i:=round(r);
	isint:= (i*1.0)=r
end;
procedure printcsv(var f:text;p:pcsv);
begin
	 if p<>nil then
	 with p^ do
	 begin
		 if tag = linestart then
		 begin
			printcsv(f,right);
			if down<> nil then
			begin
				writeln(f);
			    printcsv(f,down);
			end;
		 end
		 else
		 if tag =numeric then
		 begin
		    if isint(number) then write(f,round(number):1)
		    else write(f,number:1:6);
			if right <> nil then
			begin
				write(f,',');printcsv(f,right)
		    end

		 end
		 else
		 if tag=alpha then
		 begin

			if textual<>nil then write(f,'"',textual^,'"') else write(f,'nil');
			if right <> nil then
			begin

				write(f,',');printcsv(f,right)
		    end

		 end
	  end
 end;
function parsecsvfile(name:textfield):pcsv;
const
    megabyte =1024*1024;
    maxbuf = 30*megabyte;
type bytebuf = array[1..maxbuf] of byte;
var f:fileptr;
    bp:^bytebuf;
    fs,rc:integer;
    tokstart,tokend,currentchar:integer;
    firstfield,lastfield,firstrecord:pcsv;
    function thetoken:token;
    begin
        if currentchar <= fs then
		thetoken:=categorisor[bp^[currentchar]]
		else thetoken := EOFsym
	end;
    function peek(c:token):boolean;
    (*! matches current char against the token c returns true if it matches. *)
    begin
        peek:=c=thetoken
	end;
	function isoneof (s:tokenset):boolean;
	begin
		isoneof:=  thetoken in s
	end;
	procedure nextsymbol;
	begin
		if currentchar <= fs then currentchar := currentchar+1
	end;
	function have(c:token):boolean;
	begin
		if peek(c) then
		begin
			nextsymbol;
			have:=true;

		end
		else
		have:=false;
	end;
	function haveoneof(c:tokenset):boolean;
	begin
		if isoneof(c) then
		begin
			nextsymbol;
			haveoneof:=true;
		end
		else
		haveoneof:=false;
	end;

	procedure initialise;
	begin
	    firstfield:=nil;
	    lastfield:=nil;
	    firstrecord:=nil;

	end;
	procedure resolvealpha;
	var i,l:integer;
	begin
		with lastfield^ do
		begin
		  tag:=alpha;
		  new(textual);
		  textual^:='';
	      l:=tokend min (tokstart +textlen-1);
	      { copy field to string}
	      for i:= tokstart to l -1 do
	      begin
			textual^:=textual^ + chr(bp^[i]);
		  end;
		end;
    end;

	procedure resolvedigits;
	var i,l:integer;s:string;
	begin
		with lastfield^ do
		begin
		  tag:=numeric;
		  new(textual);
		  s:='';
	      l:=tokend min (tokstart +textlen-1);
	      { copy field to a string }
	      for i:= tokstart to l do
	      begin
			s:=s + chr(bp^[i]);
		  end;
		  val(s,number,l);{convert to binary}
		end;
    end;
	procedure resolvetoken;
	begin
		if chr(bp^[tokstart]) in ['0'..'9'] then resolvedigits
		else resolvealpha
	end;

    procedure markbegin;{ mark start of a field }
    begin
		tokstart:=currentchar;
		new(lastfield^.right);
		lastfield:=lastfield^.right;
		lastfield^.right:=nil;
	end;
	procedure markend;{marks the end of a field }
	begin
	   tokend:=currentchar;
	   resolvetoken;
	end;
	procedure setalpha(s:textfield);
	begin
		lastfield^.tag:=alpha;
		new(lastfield^.textual);
		lastfield^.textual^:=s;
	end;
    procedure emptyfield;
    begin

		markbegin;
		setalpha('');

	end;

    procedure parsebarefield;
    begin
		if isoneof([RSsym,EOFsym,FSsym]) then emptyfield
		else begin
			markbegin;
			while haveoneof([any,space]) do ; { skip over the field }
			markend;
		end;
	end;
	procedure parsedelimitedfield;
	(*! parses a field nested between " chars converting escape chars as it goes *)
	var s :textfield;i:integer;continue :boolean;
	 procedure appendcurrentchar;
	 begin
	    s:=s+chr(bp^[currentchar]);
	    nextsymbol;
	 end;
	begin
	        markbegin;
			s:='';
	        continue:=true;
	        repeat
			  while isoneof([FSsym..any]) do
			  begin
				appendcurrentchar;
			  end;
			  have(FDsym);{ eat what may be closing quotes}
			  continue := peek(FDsym) and (length (s) < textlen);
			  if continue then appendcurrentchar;
			until (not continue) ;
			setalpha(s);
	end;
    procedure parsefield;
    begin
		if have(FDsym) then parsedelimitedfield
		else parsebarefield
	end;
    procedure parserecord;
    begin
		parsefield;
		while have (FSsym) do parsefield;
	end;
    procedure parseheader;
    begin
        { claim heap space for start of first line }
        new(firstrecord);
        lastfield:=firstrecord;
        firstfield := firstrecord;
        with firstrecord^ do
        begin
			tag:=linestart;
			down:=nil;
			right:=nil;
        end;
		parserecord;
	end;
    procedure parsewholefile;
    begin
		parseheader;

		while  have(RSsym) do
		begin
		     { claim heap space for the start of the new line }
			 new(firstfield^.down);
			 firstfield:=firstfield^.down;
			 lastfield:=firstfield;
			 with firstfield^ do
	         begin
				tag:=linestart;
				down:=nil;
				right:=nil;
	         end;
			 parserecord;
		end;
	end;
begin
    initialise;
	parsecsvfile:=nil {the default case of failure};

	assign(f,name);
	reset(f);          {open file for reading}
	if ioresult=0 then {ioresult =0 if opened ok }
	begin
		fs:=filesize(f);
		if fs < maxbuf then
		begin
			new(bp);
			blockread(f,bp^[1],fs,rc);
			if rc=fs then
			begin
			  currentchar:=1;
			(*! We now have the csv file in memory - parse it *)

			   parsewholefile;
			   removetrailingnull(firstrecord);
			   parsecsvfile:=firstrecord;
			end;
			dispose(bp);
			close(f);
		end;
	end;
end;
begin
categorisor:=any;
		categorisor[FD]:=FDsym;
		categorisor[FS]:= FSsym;
		categorisor[RS]:= RSsym;
		categorisor[EOI]:=EOFsym;
		categorisor[ord(' ')]:=space;
		categorisor[CR]:=space;
		{writeln('fs=',fs,'fd=',fd,'rs=',rs);
		writeln(categorisor);}
end.
