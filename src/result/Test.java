package result;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;

import utils.ImgProcLog;

public class Test {
	private static final String RESULT_PATH = "C:\\Delin\\Updated Workspace\\iDynoMiCS\\resultss\\experiments\\";
	//private static final String RESULT_PATH = "C:\\Users\\FlannLab\\Desktop\\";
	private static String separator = "	";
	private static boolean vascular = false;

	public static void main(String[] args) throws IOException {
		/*File file = new File(RESULT_PATH);
		String[] names = file.list();
		Arrays.sort(names);
		*/// reading the latest contact folder
		//String name = names[names.length - 1];
		String name = "ExperimentADDouble_1(20160411_1949)";
		//String name = "ExperimentADDouble_1(20160411_1949)";
		//String name = "New folder";
		
		consolidateSoluteConcentrations(RESULT_PATH,name);
	}

	public static double consolidateSoluteConcentrations(String resultPath, String name) throws IOException {
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
			if (names[i].contains("480")) {
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
						if(names[i].contains("Product") && hour == 480){
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
					if (vascular && hour <= 440)
						sum = 0;
					evaluator.add(sum);
				}
				if (names[i].contains("Nutrient") && !names[i].contains("cNutrient")) {
					if (vascular && hour <= 440)
						sum = 0.0;
					nutrient.add(sum);
				}
				if (names[i].contains("Attract")) {
					if (vascular && hour >= 440)
						sum = 0.0;
					attract.add(sum);
				}
				if (names[i].contains("Product")) {
					if (vascular && hour <= 440)
						sum = 0.0;
					product.add(sum);
				}
				if (names[i].contains("Gradient")) {
					if (vascular && hour >= 440)
						sum = 0.0;
					gradient.add(sum);
				}
			}
		}
		
		List<String> outputLines = new ArrayList<String>();
		for (int i = 0; i < evaluator.size(); i++) {
			outputLines.add(460 + "," + nutrient.get(i)/(65*129) + ","
					+ product.get(i)/(65*129) + "," + evaluator.get(i)/(65*129));
//			outputLines.add(460 + "," + nutrient.get(i)/(129*257) + ","
//					+ product.get(i)/(129*257) + "," + evaluator.get(i)/(129*257));
			System.out.println(outputLines.get(i));
		}
		double totalProduct = Double.parseDouble(outputLines.get(evaluator.size()-1));
		FileUtils.writeLines(new File(resultPath + name + "\\SoluteConcentration\\Consolidated.txt"), outputLines);
		ImgProcLog.write("Product = "+product.get(product.size()-1)/(1024*512));
		ImgProcLog.write("min: "+ min);
		ImgProcLog.write("max: "+ max);
		return totalProduct;
	}
}
