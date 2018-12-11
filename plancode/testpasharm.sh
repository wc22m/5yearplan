java planning.testeconomy $1 $2
 echo  execute and time   the java harmony algorithm 
echo "time java -Xmx1512m planning.nyearHarmony testflow.csv testcap.csv testdep.csv testtarg.csv    >harmony.txt"

time java -Xmx1512m planning.nyearHarmony testflow.csv testcap.csv testdep.csv testtarg.csv    >harmony.txt
 
echo Harmony achieved by java version
 tail --lines=2 harmony.txt
 
 
echo  execute and time   the pascal harmony algorithm 
time pasharm/harmonyplan testflow.csv testcap.csv testdep.csv testtarg.csv    >pasharmony.txt
echo Harmony achieved by pascal version
tail --lines=2 pasharmony.txt 
