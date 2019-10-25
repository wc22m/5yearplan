all: harmonyplan   test harmonyplan.pdf

clean:
	rm *.asm *.mpu *.lis *.lst *.log *.vwu
	
harmonyplan: harmonyplan.pas csvfilereader.pas technologies.pas harmony.pas
	vpc  harmonyplan   -opt0   -cores1 -L
	
harmonyplan.pdf: harmonyplan.tex
	pdflatex harmonyplan.tex
	pdflatex harmonyplan.tex
	
test: harmonyplan testflow.csv testcap.csv testdep.csv testtarg.csv result.csv

result.csv:testflow.csv testcap.csv testdep.csv testtarg.csv harmonyplan
	harmonyplan testflow.csv testcap.csv testdep.csv testtarg.csv >result.csv
	
	
