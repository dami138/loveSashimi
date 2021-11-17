from selenium import webdriver

from selenium.webdriver.common.keys import Keys

import time

import urllib.request

 

driver = webdriver.Chrome('C:/Users/Public/Documents/chromedriver.exe')

driver.get("https://www.google.co.kr/imghp?hl=ko&tab=wi&ogbl")

 

#검색창 찾고, 검색어 입력, 엔터키 누름

elem = driver.find_element_by_name("q") #검색창 class로 찾아도 되고, name=q으로 찾아도 된다.

searchWord = "광어회"

elem.send_keys(searchWord) 

elem.send_keys(Keys.RETURN) #엔터키 누름

time.sleep(5)

# 스크롤을 계속 끝까지 내려서 최대한 많은 이미지를 확보

SCROLL_PAUSE_TIME = 2

 

# Get scroll height : 자바스크립트 코드로 브라우저 높이를 찾아낸다.

last_height = driver.execute_script("return document.body.scrollHeight")

 

while True:

    # Scroll down to bottom : 브라우저 끝까지 스크롤을 내리겠다.

    driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")

 

    # Wait to load page : 로딩 될 동안 time sleep한다. 위에 설정된 1초 동안

    time.sleep(SCROLL_PAUSE_TIME)

 

    # Calculate new scroll height and compare with last scroll height

    new_height = driver.execute_script("return document.body.scrollHeight")

    if new_height == last_height:

     try:

     # 스크롤 내리다 보면 '결과 더보기'버튼이 나오는데 그걸 클릭하는 부분임

     	driver.find_element_by_css_selector(".mye4qd").click()

     except: #스크롤 내리다가 맨 마지막에는 '결과 더보기'버튼이 없어서 오류가 나는데 이때 반복문을 종료한다.

     	break

    last_height = new_height

 

#작은 이미지 선택

images = driver.find_elements_by_css_selector(".rg_i.Q4LuWd")
count = 1

for image in images:

 try:

  image.click()
  time.sleep(3) # 로딩 2초 기다림

 

  #큰 이미지 주소 가져오기

  imgUrl = driver.find_element_by_xpath('//*[@id="Sva75c"]/div/div/div[3]/div[2]/c-wiz/div/div[1]/div[1]/div[2]/div[1]/a/img').get_attribute("src")
  print(imgUrl)
  #이미지 다운로드
  urllib.request.urlretrieve(imgUrl, searchWord + "_" + str(count) + ".jpg")

  count = count + 1

 except: #오류가 나면 무시하고 다음으로 넘어가라

  pass

 

#웹브라우저를 닫아준다.

driver.close()