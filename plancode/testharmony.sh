 
 java planning.testeconomy $1 $2
 echo  execute and time   the harmony algorithm 
echo "time java -Xmx1512m planning.nyearHarmony testflow.csv testcap.csv testdep.csv testtarg.csv    >harmony.txt"

time java -Xmx1512m planning.nyearHarmony testflow.csv testcap.csv testdep.csv testtarg.csv    >harmony.txt
 
echo Harmony achieved 
 tail --lines=2 harmony.txt
 echo   prepare the linear programme and print time taken
 
time  java planning.nyearplan testflow.csv testcap.csv testdep.csv testtarg.csv   >test.lp
 
echo statistics of the linear programme specification
echo lines, words, chars
wc test.lp  
echo execute and time  the linear programme
echo "time lp_solve <test.lp|sort >test.txt "
time lp_solve <test.lp|sort >test.txt 
 
echo Degree of plan fulfillment using linear programme
tail --lines=2 test.txt 

