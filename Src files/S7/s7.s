int g1 = -10;
int garr[3];
int g2 = -50;

int x = 10;
int y = 20;
int z;

void main() {
	println("===Basic array test===");
	basicArrayTest();
	println("===Global array test===");
	globalArrayTest();
	println("===For loop test===");
	forLoopTest();
	println("===Formatting print test===");
	formattedPrintTest();
	println();
}

void basicArrayTest() {
	int i;
	int a[5];
	int x = 5;

	a[1] = 20;
	a[2] = 30;
	a[3] = 200;
	a[4] = 5000;
	a[5] = 2;

	i = 1;
	x = a[0]; // x is size of array
	while (x > 0) {
		a[x] = a[x] + 1;
		println(a[x]);
		x = x - 1;
	}
}

void globalArrayTest() {
	int i;
	garr[1] = 20;
	garr[2] = 30;
	garr[3] = 40;

	println("printing garr:");
	i = 0;
	while (i <= garr[0]) {
		println(garr[i]);
		i = i + 1;
	}

	println("Bounds checking:");
	print("g1 -10 = ");
	println(g1);

	print("g2 -50 = ");
	println(g2);

	println("Out of bounds test:");
	println(garr[-1]);
	println(garr[4]);
}

void forLoopTest()
{
	for(int i = 0; i <= 3; i = i + 1){
		//if(i == 2) break;
		print("loop ");
		println(i);
	}
}

void formattedPrintTest()
{
	z = x+y;
   print("The result of {0} + {1} = {2}",x,y,z);
}