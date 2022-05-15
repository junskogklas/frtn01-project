# Importing all modules
import time
import cv2
from client import Client
import imutils
from imutils import contours
from imutils import perspective
from scipy.spatial import distance as dist
import numpy as np

# Importing relevant socket communication packages
from socket import socket
from socket import AF_INET
from socket import SOCK_STREAM
from socket import SHUT_RDWR

def nothing(x):
    pass
def midpoint(p1,p2):
	return ((p1[0]+p2[0]*0.5),(p1[1]+p2[1]*0.5))
# capture from usb cam
webcam_video = cv2.VideoCapture(1)
# Capturing from webcam 
#webcam_video = cv2.VideoCapture(0)

cv2.namedWindow('Mask bar')

cv2.createTrackbar('RUpper','Mask bar',0,255,nothing) 
cv2.createTrackbar('GUpper','Mask bar',0,255,nothing)
cv2.createTrackbar('BUpper','Mask bar',0,255,nothing)
cv2.createTrackbar('RLower','Mask bar',0,255,nothing)
cv2.createTrackbar('GLower','Mask bar',0,255,nothing)
cv2.createTrackbar('BLower','Mask bar',0,255,nothing)
#cv2.createTrackbar('Gaussian Blur','Mask bar',1,9,nothing)
# Set default value for MAX AND LOW RGB trackbars.
cv2.setTrackbarPos('RUpper','Mask bar',179)
cv2.setTrackbarPos('GUpper','Mask bar',1179)
cv2.setTrackbarPos('BUpper','Mask bar',179)
cv2.setTrackbarPos('RLower','Mask bar',75)
cv2.setTrackbarPos('GLower','Mask bar',45)
cv2.setTrackbarPos('BLower','Mask bar',0)
#cv2.setTrackbarPos('Gaussian Blur','image',1)

# Establish connection to Server
conn = Client()
conn.connect(4444) # IP-address implicit
print('Connection to host established')

# Gaussian blur magnitude
blur = 7

# Size of reference object
width = 2.5

colors = [0,0,255]
while True:
	rUpper = cv2.getTrackbarPos('RUpper','Mask bar')
	gUpper = cv2.getTrackbarPos('GUpper','Mask bar')
	bUpper = cv2.getTrackbarPos('BUpper','Mask bar')
	rLower = cv2.getTrackbarPos('RLower','Mask bar')
	gLower = cv2.getTrackbarPos('GLower','Mask bar')
	bLower = cv2.getTrackbarPos('BLower','Mask bar')
	#blur = cv2.getTrackbarPos('Gaussian Blur','Mask bar')
	
	lower = np.array([bLower,gLower,rLower])
	upper = np.array([bUpper,gUpper,rUpper])
	
	success, video = webcam_video.read() # Reading webcam footage
	
	video = cv2.resize(video,None,fx=0.5,fy=0.5,interpolation=cv2.INTER_AREA)
	img = cv2.cvtColor(video, cv2.COLOR_BGR2HSV) # Converting BGR image to HSV format

	# Filter with gaussian noise
	img = cv2.GaussianBlur(img,(blur,blur),0)

	# Dilation + erosion to close gaps in between object edges
	img = cv2.dilate(img,None,iterations = 1)
	img = cv2.erode(img,None, iterations = 1)
	
	mask = cv2.inRange(img, lower, upper) # Masking the image to find our color
	
	items = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE) # Finding contours in mask image
	mask_contours = imutils.grab_contours(items)
	
	# If findcountours doesn't find anything, theres nothing to unpack from grab_contours
	if len(mask_contours) !=0:
		# Sort contours, minseg autobalancing should be the left-most object
	    (mask_contours,_) = contours.sort_contours(mask_contours)
	
	# Finding position of contours
	refObj = None
	if len(mask_contours) != 0:
		for mask_contour in mask_contours:
			if cv2.contourArea(mask_contour) < 600:
				continue
			box = cv2.minAreaRect(mask_contour)
			box = cv2.boxPoints(box)
			box = np.array(box,dtype = "int")
			box = perspective.order_points(box)
			
			cX = np.average(box[:,0])
			cY = np.average(box[:,1])
		
			if refObj is None:
				# tl = top left etc
				(tl, tr, br, bl) = box
				(tlblX, tlblY) = midpoint(tl, bl)
				(trbrX, trbrY) = midpoint(tr, br)
				# Compute the Euclidean distance between the midpoints,
				# then construct the reference object
				D = dist.euclidean((tlblX, tlblY), (trbrX, trbrY))
				refObj = (box, (cX, cY), D / width )
				continue
		
	orig = video.copy()
	if refObj != None:
		cv2.drawContours(orig, [refObj[0].astype("int")], -1, (0, 255, 0), 2)
		cv2.drawContours(orig, [box.astype("int")], -1, (0, 255, 0), 2)
		refCoords = np.vstack([refObj[0], refObj[1]])
		objCoords = np.vstack([box, (cX, cY)])
		refX, refY = refObj[1]
		cv2.circle(orig,(int(cX),int(cY)),5,[0,0,255],-1)
		cv2.circle(orig,(int(refX),int(refY)),5,[0,0,255],-1)
		cv2.line(orig,(int(refX),int(refY)),(int(cX),int(cY)),[0,0,255],2)
		D = dist.euclidean((refX, refY), (cX, cY)) / refObj[2]
		(mX, mY) = midpoint((refX, refY), (cX, cY))
		cv2.putText(orig, "{:.1f}cm".format(D), (int(mX+10), int(mY + 10)),cv2.FONT_HERSHEY_SIMPLEX, 0.55, [0,0,255], 2)

	
	cv2.imshow("mask image", mask) # Displaying mask image
	cv2.imshow("window", orig) # Displaying webcam image
	conn.send(str(D))
	echo = conn.receive()
	if echo != 'Ok':
	  print("Something went wrong in receiving")
	#time.sleep(0.1)
	
	c = cv2.waitKey(1)
	if c == 27:
		print("Closing down")	
		break
webcam_video.release()
cv2.destroyAllWindows()

