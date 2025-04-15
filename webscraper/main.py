import datetime
import json
from typing import List
import requests
from bs4 import BeautifulSoup
from dataclasses import asdict, dataclass


@dataclass
class Review:

    stars: int
    text: str


@dataclass
class Book:

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


def scrape_book(url: str) -> Book:
    test = requests.get(url)
    soup = BeautifulSoup(test.text, "html.parser")

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
    firstPublishedDate = firstPublished.text[16:]
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
        newReviews.append(Review(stars, cleanedText))  # type: ignore
    image_tag = soup.find("img", {"class": "ResponsiveImage"})
    image_src = image_tag["src"]  # type: ignore
    if image_src is None:
        raise Exception("Image not found")

    book = Book(
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
    for page in range(1, 2):

        headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
        }
        test = requests.get(
            f"https://www.goodreads.com/list/show/1.Best_Books_Ever?page={page}",
            headers=headers,
        )
        # print(test)
        soup = BeautifulSoup(test.text, "html.parser")

        books = soup.find_all("tr", {"itemtype": "http://schema.org/Book"})
        # print(test.text)
        bookLinks = ["https://www.goodreads.com" + book.find("a", {"class": "bookTitle"})["href"] for book in books]  # type: ignore
        for link in bookLinks:
            book = scrape_book(link)
            book_dict = asdict(book)

            book_json = json.dumps(book_dict, indent=4)

            print(book_json)


if __name__ == "__main__":
    scrape_best_books()
    # book = scrape_book(
    #     "https://www.goodreads.com/book/show/198902277-the-wedding-people?from_choice=true"
    # )
    # book_dict = asdict(book)

    # book_json = json.dumps(book_dict, indent=4)

    # print(book_json)
