from concurrent.futures import ThreadPoolExecutor
import datetime
import json
from typing import List
import requests
from bs4 import BeautifulSoup
from dataclasses import asdict, dataclass
from elasticsearch import Elasticsearch, NotFoundError

client = Elasticsearch(
    hosts=["http://localhost:9200"],
    verify_certs=False,
    basic_auth=["elastic", "mWQ787fk"],  # type: ignore
    # api_key= ALTERNATIVE (USE ENCODED VERSION)
)


@dataclass
class Review:

    stars: int
    # text: str


@dataclass
class Book:
    url: str
    title: str
    author: str
    authorLink: str
    imageUrl: str
    rating: float
    ratingsCount: int
    reviewCount: int
    description: str
    genres: List[str]
    pages: str
    firstPublished: int
    reviews: List[Review]


def scrape_book(url: str) -> Book | None:
    print(url)
    test = requests.get(url)

    soup = BeautifulSoup(test.text, "html.parser")

    if soup.find("div", {"class": "ErrorPage__top"}):
        print("404")
        return None

    titleElement = soup.find("h1", {"data-testid": "bookTitle"})
    if titleElement is None:
        raise Exception("Book title not found.")
    title = titleElement.get_text()

    authorElement = soup.find("span", {"data-testid": "name"})
    if authorElement is None:
        raise Exception("Author not found.")
    author = authorElement.get_text()

    authorLink = soup.find("a", {"class": "ContributorLink"})
    if authorLink is None:
        raise Exception("Author link not found.")
    authorLinkHref = authorLink["href"]  # type: ignore

    ratingElement = soup.find("div", {"class": "RatingStatistics__rating"})
    if ratingElement is None:
        raise Exception("Rating not found.")
    rating = float(ratingElement.get_text())

    ratingCount = soup.find("span", {"data-testid": "ratingsCount"})
    if ratingCount is None:
        raise Exception("Ratings count not found.")
    ratingsCount = int(ratingCount.text.replace(",", "").split()[0])

    reviewCount = soup.find("span", {"data-testid": "reviewsCount"})
    if reviewCount is None:
        raise Exception("Review count not found.")
    reviewsCount = int(reviewCount.text.replace(",", "").strip().split()[0])

    description = soup.find("div", {"data-testid": "description"})
    if description is None:
        raise Exception("Description not found.")
    try:
        description.i.decompose()  # type: ignore
    except:
        pass
    descriptionText = description.text.strip()

    genres = soup.find_all("span", {"class": "BookPageMetadataSection__genreButton"})
    if not genres:
        raise Exception("Genres not found.")
    genresList = [genre.text for genre in genres]

    pages = soup.find("p", {"data-testid": "pagesFormat"})
    if pages is None:
        raise Exception("Pages not found.")
    pagesText = pages.text.split(" ")[0]

    firstPublished = soup.find("p", {"data-testid": "publicationInfo"})
    if firstPublished is None:
        raise Exception("First published date not found.")
    firstPublishedDate = firstPublished.text.lower().split("published")[1].strip()

    if len(firstPublishedDate.split(", ")[1]) < 4:
        # When the date for earlier than 1000
        firstPublishedDate = (
            firstPublishedDate.split(", ")[0]
            + ", "
            + firstPublishedDate.split(", ")[1].zfill(4)
        )
    firstPublishedUnix = int(
        datetime.datetime.strptime(firstPublishedDate, "%B %d, %Y").timestamp()
    )

    reviews = soup.find_all("section", {"class": "ReviewCard__content"})
    if not reviews:
        raise Exception("Reviews not found.")
    newReviews = []
    for review in reviews:
        reviewText = review.find("span", {"class": "Formatted"})  # type: ignore
        stars = len(review.find_all("span", {"class": "baseClass RatingStar--small"}))  # type: ignore
        if reviewText is None:
            raise Exception("A review text was not found.")
        cleanedText = reviewText.text  # type: ignore
        newReviews.append(Review(stars))
    image_tag = soup.find("img", {"class": "ResponsiveImage"})
    image_src = image_tag["src"]  # type: ignore
    if image_src is None:
        raise Exception("Image not found")

    book = Book(
        url=url,
        title=title,
        author=author,
        authorLink=authorLinkHref,  # type: ignore
        rating=rating,
        ratingsCount=ratingsCount,
        reviewCount=reviewsCount,
        description=descriptionText,
        genres=genresList,
        pages=pagesText,
        firstPublished=firstPublishedUnix,
        reviews=newReviews,
        imageUrl=str(image_src),
    )

    return book


def scrape_best_books():
    linksToProcess = []
    with ThreadPoolExecutor(max_workers=50) as executor:
        result = list(executor.map(process_page, [page for page in range(1, 101)]))
        # result is a list of list of urls to books
        for bookList in result:
            for bookUrl in bookList:
                linksToProcess.append(bookUrl)
    with ThreadPoolExecutor(max_workers=50) as executor:
        executor.map(scrape_and_process_book, linksToProcess)


def process_page(page: int) -> List[str]:
    print(page)
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    }
    test = requests.get(
        f"https://www.goodreads.com/list/show/1.Best_Books_Ever?page={page}",
        headers=headers,
    )
    soup = BeautifulSoup(test.text, "html.parser")

    books = soup.find_all("tr", {"itemtype": "http://schema.org/Book"})

    bookLinks = ["https://www.goodreads.com" + book.find("a", {"class": "bookTitle"})["href"] for book in books]  # type: ignore

    # filteredLinks = [link for link in bookLinks if not document_exists(link)]

    return bookLinks


def scrape_and_process_book(link: str):
    try:
        book = scrape_book(link)
    except:
        print(f"Book: {link} failed to scrape")
        return

    if book:
        # pass
        sendToElastic(book)


def sendToElastic(book: Book):

    if not client.ping():
        raise Exception("could not connect to elasticsearch")

    book_dict = asdict(book)

    client.index(index="books", body=book_dict)


def document_exists(url: str) -> bool:
    # does a query to check if a book with the exact url already exists
    # it's used so the same book does not get scraped twice
    query = {"query": {"term": {"url.keyword": url}}}
    try:
        response = client.search(index="books", body=query)
    except NotFoundError:
        # occurs for the first book when the book index has not been created yet
        return False
    result = response["hits"]["total"]["value"] > 0
    if result:
        print("Book already in database")
    return result


if __name__ == "__main__":
    scrape_best_books()
