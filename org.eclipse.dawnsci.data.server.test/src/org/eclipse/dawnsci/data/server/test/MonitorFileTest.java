package org.eclipse.dawnsci.data.server.test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.eclipse.dawnsci.analysis.api.dataset.DataEvent;
import org.eclipse.dawnsci.analysis.api.dataset.IDataListener;
import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.analysis.api.dataset.IRemoteDataset;
import org.eclipse.dawnsci.analysis.dataset.impl.Random;
import org.eclipse.dawnsci.data.client.DataClient;
import org.eclipse.dawnsci.data.server.Format;
import org.eclipse.dawnsci.plotting.api.histogram.IImageService;
import org.eclipse.dawnsci.plotting.api.histogram.ImageServiceBean;
import org.eclipse.swt.graphics.ImageData;
import org.junit.Test;

/**
 * Test which writes to a local file random data every timeinterval
 * and checks that the DataServer sents events.
 * 
 * @author fcp94556
 *
 */
public class MonitorFileTest {
	
	private static IImageService iservice;
	
	
	public static IImageService getImageService() {
		return iservice;
	}

	public static void setImageService(IImageService iservice) {
		MonitorFileTest.iservice = iservice;
	}

	private volatile boolean testIsRunning = false;

	@Test
	public void testDirectoryMonitoring() throws Exception {

	}
	
	@Test
	public void testFileMonitoring() throws Exception {
		
		try {
			testIsRunning = true;
			final File tmpData = startFileWritingThread();
			
			DataClient<IRemoteDataset> client = new DataClient<IRemoteDataset>("localhost", 8080);
			client.setPath(tmpData.getAbsolutePath());
			client.setDataset(null); // We just get the first image in the PNG file.
			client.setFormat(Format.MONITOR); // If the file changes, we get dynamic dataset events.
			
			IRemoteDataset data = client.get();
			data.addDataListener(new IDataListener() {
				@Override
				public void dataChangePerformed(DataEvent evt) {
					System.out.println("Data changed, new shape is "+evt.getShape());
				}
			});

			Thread.sleep(5000);
			
		} finally {
			testIsRunning = false;
		}
	}

	private File startFileWritingThread() throws IOException {
		
        final File ret = File.createTempFile("temp_transient_file", ".png");
        ret.deleteOnExit();
        
        final Thread runner = new Thread(new Runnable() {
        	public void run() {
        		
        		while(testIsRunning) {
        			
        			try {
	        			IDataset       rimage = Random.rand(new int[]{1024, 1024});
	        			ImageServiceBean bean = iservice.createBeanFromPreferences();
	        			bean.setImage(rimage);
	        			final ImageData   data = iservice.getImageData(bean);
	        			final BufferedImage bi = iservice.getBufferedImage(data);
	        			
	        			ImageIO.write(bi, "PNG", ret);
	        			
	        			Thread.sleep(200);
	        			System.out.println(">> Updated "+ret.getAbsolutePath());
	        			
        			} catch (Exception ne) {
        				ne.printStackTrace();
        				break;
        			}
        			
        		}
        	}
        });
        runner.setPriority(Thread.NORM_PRIORITY);
        runner.setDaemon(true);
        runner.start();
        return ret;
	}
}