(ns toutiao.learn.opencv
  (:require [clojure.java.io :as io])
  (:import [org.opencv.core Mat MatOfRect Point Scalar Size CvType]
           [org.opencv.videoio VideoCapture]
           [org.opencv.imgcodecs Imgcodecs]
           [org.opencv.imgproc Imgproc]
           [org.opencv.objdetect CascadeClassifier]))

(clojure.lang.RT/loadLibrary org.opencv.core.Core/NATIVE_LIBRARY_NAME)

(org.opencv.core.Point. 0 0)


(defn img->gray [f]
  (let [img (Imgcodecs/imread f)]
    (Imgproc/cvtColor img img Imgproc/COLOR_RGB2GRAY)
    (Imgcodecs/imwrite "/Users/huangyesheng/Documents/toutiao/resources/public/img/example0-gray.png" img)
    (println "generate generate example0-gray.png")))

(defn face-detect [f]
  (let [img (Imgcodecs/imread f)
        gray (Mat.)
        faces (MatOfRect.)]

    ;; Convert image to graylevel and equalize the histogram
    (Imgproc/cvtColor img gray Imgproc/COLOR_RGB2GRAY)
    (Imgproc/equalizeHist gray gray)

    ;; Load the classifier file from
    ;; https://github.com/nagadomi/lbpcascade_animeface
    (doto (CascadeClassifier.)
      (.load (.getPath (io/resource "opencv/lbpcascade_animeface.xml")))
      (.detectMultiScale gray faces))

    ;; Draw rectangle according to face size
    (doseq [face (.toList faces)]
      (Imgproc/rectangle img
                         (Point. (.x face) (.y face))
                         (Point. (+ (.x face) (.width face))
                                 (+ (.y face) (.height face)))
                         (Scalar. 144 48 255)
                         2))

    ;; Write the result
    (Imgcodecs/imwrite "resources/public/img/example1-fdetect.png" img)
    (println "generate generate example1-fdetect.png")))

(img->gray "/Users/huangyesheng/Documents/toutiao/resources/public/img/2719544-5f38ee75812cfb2c.jpg")

(face-detect "/Users/huangyesheng/Documents/toutiao/resources/public/img/1112.png")

