all:   planning/nyearplan.class plan.txt test.txt result.csv

test.txt: planning/nyearplan.class planning/testeconomy.class testrun.sh
	bash testrun.sh 4 2

harmony.txt: planning/nyearplan.class planning/testeconomy.class testharmony.sh planning/nyearHarmony.class testharmony.sh Makefile
	bash testharmony.sh 3 3

pasharm/harmonyplan:  pasharm/harmonyplan.pas pasharm/csvfilereader.pas pasharm/technologies.pas pasharm/harmony.pas
	$(MAKE) -C pasharm
	

planning/csvfilereader.class: planning/csvfilereader.java planning/pcsv.java
	javac planning/csvfilereader.java
	
clean:
	rm planning/*.class  
	rm plan.txt
	rm model.lp
	
planning/nyearplan.class:planning/nyearplan.java planning/csvfilereader.class
		javac planning/nyearplan.java
		
plan.txt: model.lp
	lp_solve <model.lp|sort >plan.txt
	
model.lp:planning/nyearplan.class flows.csv cap.csv dep.csv labtarg.csv Makefile
	java planning.nyearplan flows.csv cap.csv dep.csv labtarg.csv   >model.lp
	
hplan.txt:  planning/nyearplan.class  planning/nyearHarmony.class    Makefile
	java planning.nyearHarmony flows.csv cap.csv dep.csv labtarg.csv   >hplan.txt
	
planning/testeconomy.class:planning/testeconomy.java
	javac planning/testeconomy.java

planning/nyearHarmony.class:planning/Harmonizer.java planning/TechnologyComplex.java planning/JointProductionTechnique.java planning/nyearHarmony.java planning/Technique.java
	javac planning/nyearHarmony.java

result.csv:testflow.csv testcap.csv testdep.csv testtarg.csv planning/nyearHarmony.class
	java planning.nyearHarmony testflow.csv testcap.csv testdep.csv testtarg.csv >result.csv
	tail --lines=2 result.csv

pasresult.csv:testflow.csv testcap.csv testdep.csv testtarg.csv pasharm/harmonyplan
	pasharm/harmonyplan testflow.csv testcap.csv testdep.csv testtarg.csv >pasresult.csv
	tail --lines=2 pasresult.csv
