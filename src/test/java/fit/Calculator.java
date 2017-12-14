package fit;

public class Calculator extends ColumnFixture {
	public int value1;
	public int value2;
	public String operator;

	public int result() {
		int result;
		switch (operator) {
		case "+":
			result = value1 + value2;
			break;
		case "-":
			result = value1 - value2;
			break;
		case "*":
			result = value1 * value2;
			break;
		case "/":
			result = value1 / value2;
			break;
		default:
			throw new IllegalArgumentException("unknown operator " + operator);
		}

		return result;
	}
}
