package result;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;

import simulation.Controller;
import utils.ImgProcLog;

public class Test {
	private static String separator = "	";
	private static boolean vascular = false;


	/**
	 * Calculates different solute amounts of the cell factory and returns the total amount of product produced
	 * @param resultPath The path to the current project
	 * @param name Name of the folder in which results of the project are located
	 * @return Total product produced in this cell factory
	 * @throws IOException
	 */
	public static double consolidateSoluteConcentrations(String resultPath, String name, int numIteration) throws IOException {
		int iterationsIndex = numIteration * 20;
		String newIterationIndex = String.valueOf(iterationsIndex + 40); 
		double finalProduct =0;
		String[] names;
		File lastResultDirectory = new File(resultPath + name + "\\SoluteConcentration\\xy-1\\");
		names = lastResultDirectory.list();
		Arrays.sort(names);
		List<Double> evaluator = new ArrayList<Double>();
		List<Double> product = new ArrayList<Double>();
		List<Double> nutrient = new ArrayList<Double>();
		List<Double> gradient = new ArrayList<Double>();
		List<Double> attract = new ArrayList<Double>();
		double min = Integer.MAX_VALUE;
		double max  = 0;
		for (int i = 0; i < names.length; i++) {
			if (names[i].contains(newIterationIndex)) {
				String[] splits = names[i].split("\\.");
				int hour = Integer.parseInt(splits[0].substring(splits[0].length() - 3, splits[0].length()));
				//System.out.println(hour);
				String filePath = resultPath + name + "\\SoluteConcentration\\xy-1\\" + names[i];
				List<String> lines = Files.readAllLines(Paths.get(filePath));

				double sum = 0;
				for (int k = lines.size() - 1; k >= 0; k--) {
					String[] digitStrings = lines.get(k).split(separator);
					for (int j = 0; j < digitStrings.length; j++) {
						sum += Double.parseDouble(digitStrings[j]);
						if(names[i].contains("Product") && hour == Integer.parseInt(newIterationIndex)){
							double temp = Double.parseDouble(digitStrings[j]);
							if(temp<min){
								min = temp;
							}
							if(temp>max){
								max = temp;
							}
						}
					}
				}
				if (names[i].contains("Evaluator")) {
					if (vascular && hour <= iterationsIndex)
						sum = 0;
					evaluator.add(sum);
				}
				if (names[i].contains("Nutrient") && !names[i].contains("cNutrient")) {
					if (vascular && hour <= iterationsIndex)
						sum = 0.0;
					nutrient.add(sum);
				}
				if (names[i].contains("Attract")) {
					if (vascular && hour >= iterationsIndex)
						sum = 0.0;
					attract.add(sum);
				}
				if (names[i].contains("Product")) {
					if (vascular && hour <= iterationsIndex)
						sum = 0.0;
					product.add(sum);
				}
				if (names[i].contains("Gradient")) {
					if (vascular && hour >= iterationsIndex)
						sum = 0.0;
					gradient.add(sum);
				}
			}
		}
		
		List<String> outputLines = new ArrayList<String>();
		for (int i = 0; i < evaluator.size(); i++) {
			outputLines.add(460 + "," + nutrient.get(i)/(65*129) + ","
					+ product.get(i)/(65*129) + "," + evaluator.get(i)/(65*129));
			ImgProcLog.write(Controller.getCurrentDir(), "Result: "+ outputLines);
		}
		finalProduct = evaluator.get(0)/(65*129);
		ImgProcLog.write(Controller.getCurrentDir(), "min: "+ min);
		ImgProcLog.write(Controller.getCurrentDir(), "max: "+ max);
		ImgProcLog.write(Controller.getCurrentDir(), "product = "+ finalProduct);
		FileUtils.writeLines(new File(resultPath + name + "\\SoluteConcentration\\Consolidated.txt"), outputLines);
		return finalProduct;
	}
}
