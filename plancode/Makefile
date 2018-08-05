all:   planning/nyearplan.class plan.txt

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
	
model.lp:planning/nyearplan.class flows.csv cap.csv dep.csv labtarg.csv makefile
	java planning.nyearplan flows.csv cap.csv dep.csv labtarg.csv   >model.lp
	
	
