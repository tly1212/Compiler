S7 includes the following new features:

1.Array
	I added static arrays to S7, in which the user may define global or local arrays with a static size. They may then access the values of the arrays and assign values to these arrays.

	Similarly to C, arrays can be defined either globally or locally as follows	where <UNSIGNED> is the size of the array:

		int arr[<UNSIGNED>];

	The programmer can then assign values to the array using the following statement where the first EXPR() defines the item in the array to update and the second defines the new value at that location.

	arr[EXPR()] = EXPR();

	Finally, the EXPR() production was extended to allow accessing values from an array using the normal syntax. The following shows an expression retrieving the value in the array at the location EXPR() results in.

	arr[EXPR()]
 
2.Format print
	Our S7 supports formatting of the print and println statements using arguments.
	For example print("Sum of {0} + {1} = {2} ",x,y,z);
	Here the first argument takes the place {0}, Second argument takes the place {1} and the third argument takes the place {2}.
	For example the following code is supported in our S7
	int x =5;
	int y =6;
	int z;
	void main()
	{
		t();
	}
	void t()
	{
		z =x*y;
		print("Product of {0} * {1} = {2}",x,y,z);
	}

	The output of the code will be : Product of 5*6 = 30.
	The code also supports passing direct integers as arguments. 
	For example the following code will produce the same output as the previous one.

	void main()
	{
		t();
	}
	void t()
	{
		print("Product of {0} * {1} = {2}",5,6,30);
	}

	The number of arguments can be arbitrary but we have to make sure we have the corresponding {k} places in the String.
	So the number of arguments can vary from 0 to any number.

3. For loop
	S7 support the for loop with the form:
		for(int i = 1; i < k; i = i + 1)
			{
				//content
			}
	
	the first expression in parenthesis can be parsed as local declearation, the second expression can be parsed by using the compare function we implmentd in S6 and the last expression can not be parsed by calling the assingmentStatement becaused the is no semicolon following it. The way I take is first parse the <ID> and then "=", finally call the expr() method.