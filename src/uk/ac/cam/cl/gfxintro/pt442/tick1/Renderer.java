package uk.ac.cam.cl.gfxintro.pt442.tick1;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Vector;

public class Renderer {
	
	// The width and height of the image in pixels
	private int width, height;
	
	// Bias factor for reflected and shadow rays
	private final double EPSILON = 0.0001;

	// The number of times a ray can bounce for reflection
	private int bounces;
	
	// Background colour of the image
	private ColorRGB backgroundColor = new ColorRGB(0.001);

	public Renderer(int width, int height, int bounces) {
		this.width = width;
		this.height = height;
		this.bounces = bounces;
	}

	/*
	 * Trace the ray through the supplied scene, returning the colour to be rendered.
	 * The bouncesLeft parameter is for rendering reflective surfaces.
	 */
	protected ColorRGB trace(Scene scene, Ray ray, int bouncesLeft) {

		// Find closest intersection of ray in the scene
		RaycastHit closestHit = scene.findClosestIntersection(ray);

        // If no object has been hit, return a background colour
        SceneObject object = closestHit.getObjectHit();
        if (object == null){
            return backgroundColor;
        }
        
        // Otherwise calculate colour at intersection and return
        // Get properties of surface at intersection - location, surface normal
        Vector3 P = closestHit.getLocation();
        Vector3 N = closestHit.getNormal();
        Vector3 O = ray.getOrigin();

		// Calculate direct illumination at the point
		ColorRGB directIllumination = this.illuminate(scene, object, P, N, O);

		// Get reflectivity of object
		double reflectivity = object.getReflectivity();

		//return directIllumination;

		if (bouncesLeft == 0 || reflectivity == 0) {
			// Base case - if no bounces left or non-reflective surface
			return directIllumination;
		} else { // Recursive case
			ColorRGB reflectedIllumination;

			Vector3 R = (((ray.getDirection()).scale(-1)).reflectIn(N.normalised())).normalised();
			Ray reflectedRay = new Ray(P.add(N.scale(EPSILON)), R);
			reflectedIllumination = trace(scene, reflectedRay, (bouncesLeft-1));

			// Scale direct and reflective illumination to conserve light
			directIllumination = directIllumination.scale(1.0 - reflectivity);
			reflectedIllumination = reflectedIllumination.scale(reflectivity);

			// Return total illumination
			return directIllumination.add(reflectedIllumination);

		}
	}

	/*
	 * Illuminate a surface on and object in the scene at a given position P and surface normal N,
	 * relative to ray originating at O
	 */
	private ColorRGB illuminate(Scene scene, SceneObject object, Vector3 P, Vector3 N, Vector3 O) {
	   
		ColorRGB colourToReturn = new ColorRGB(0);

		ColorRGB I_a = scene.getAmbientLighting(); // Ambient illumination intensity

		ColorRGB C_diff = object.getColour(); // Diffuse colour defined by the object
		
		// Get Phong coefficients
		double k_d = object.getPhong_kD();
		double k_s = object.getPhong_kS();
		double alpha = object.getPhong_alpha();

		ColorRGB ambient = C_diff.scale(I_a);

		colourToReturn = (ambient);

		// Loop over each point light source
		List<PointLight> pointLights = scene.getPointLights();
		for (int i = 0; i < pointLights.size(); i++) {
			PointLight light = pointLights.get(i); // Select point light

			// Calculate point light constants
			double distanceToLight = light.getPosition().subtract(P).magnitude();
			ColorRGB C_spec = light.getColour();
			ColorRGB I = light.getIlluminationAt(distanceToLight);

			// Calculate unit direction to the point light from the surface point
			Vector3 unitL_i = light.getPosition().subtract(P).normalised();
			// Calculate unit surface normal at point
			Vector3 unitN = N.normalised();
			// Calculate N dot L
			double NdotL = unitL_i.dot(unitN);
			// Calculate diffuse component
			ColorRGB diffuse = C_diff.scale(k_d).scale(I).scale(Math.max(0, NdotL));


			// Calculate reflection of l_i
			Vector3 unitR = unitL_i.reflectIn(unitN);
			// Calculate the view vector (pointing to either the camera or the point of reflection)
			Vector3 unitV = O.subtract(P).normalised();
			// Calculate r dot v
			double RdotV = unitR.dot(unitV);
			// Calculate specular component
			ColorRGB specular = C_spec.scale(k_s).scale(I).scale(Math.pow((Math.max(0, RdotV)), alpha));

			Ray shadowRay = new Ray(P.add(N.scale(EPSILON)), unitL_i);
			RaycastHit hit = scene.findClosestIntersection(shadowRay);
			Vector3 intersectionLocation = hit.getLocation();
			double distanceToIntersection;
			try {
				distanceToIntersection = P.subtract(intersectionLocation).magnitude();
			} catch (Exception e) {
				distanceToIntersection = Double.POSITIVE_INFINITY;
			}
			if (distanceToIntersection > distanceToLight) {
				colourToReturn = colourToReturn.add(diffuse);
				colourToReturn = colourToReturn.add(specular);
			}
		}
		return colourToReturn;
	}

	// Render image from scene, with camera at origin
	public BufferedImage render(Scene scene) {
		
		// Set up image
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		
		// Set up camera
		Camera camera = new Camera(width, height);

		// Loop over all pixels
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				Ray ray = camera.castRay(x, y); // Cast ray through pixel
				ColorRGB linearRGB = trace(scene, ray, bounces); // Trace path of cast ray and determine colour
				ColorRGB gammaRGB = tonemap( linearRGB );
				image.setRGB(x, y, gammaRGB.toRGB()); // Set image colour to traced colour
			}
			// Display progress every 10 lines
            if( y % 10 == 9 | y==(height-1) )
			    System.out.println(String.format("%.2f", 100 * y / (float) (height - 1)) + "% completed");
		}
		return image;
	}


	// Combined tone mapping and display encoding
	public ColorRGB tonemap( ColorRGB linearRGB ) {
		double invGamma = 1./2.2;
		double a = 2;  // controls brightness
		double b = 1.3; // controls contrast

		// Sigmoidal tone mapping
		ColorRGB powRGB = linearRGB.power(b);
		ColorRGB displayRGB = powRGB.scale( powRGB.add(Math.pow(0.5/a,b)).inv() );

		// Display encoding - gamma
		ColorRGB gammaRGB = displayRGB.power( invGamma );

		return gammaRGB;
	}


}
