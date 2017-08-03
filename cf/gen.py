topic = [30,40,50,60,70,80]
inT = [0.2,0.4,0.5,0.6,0.8]
example = open('example.json')
gFile = open('group.sh','w')
for t in topic:
    for r in inT:
        inTopic = int(t * r)
        configFileName = 'if.' + str(t) +'.' + str(inTopic)
        gFile.write('sh clean.sh\njava -Xmx102400m -jar UISMF.jar cf/' + configFileName + '\n');
        with open(configFileName, 'w') as file:
            for line in example:
                line = line.strip()
                if 'topicCount' in line:
                    file.write('"topicCount": '+ str(t)+',\n')
                elif 'interestTopicCount' in line:
                    file.write('"interestTopicCount": '+ str(inTopic)+',\n')
                elif 'ldaThreadHold' in line:
                    file.write('"ldaThreadHold": '+ str(float(1)/t)+',\n')
                else:
                    file.write(line + '\n')
            example.seek(0)
gFile.close()